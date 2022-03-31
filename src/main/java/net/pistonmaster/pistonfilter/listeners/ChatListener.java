package net.pistonmaster.pistonfilter.listeners;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import lombok.RequiredArgsConstructor;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonchat.api.PistonChatEvent;
import net.pistonmaster.pistonchat.api.PistonWhisperEvent;
import net.pistonmaster.pistonchat.utils.CommonTool;
import net.pistonmaster.pistonchat.utils.UniqueSender;
import net.pistonmaster.pistonfilter.PistonFilter;
import net.pistonmaster.pistonfilter.utils.*;
import okhttp3.*;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ChatListener implements Listener {
    private final PistonFilter plugin;
    private final Map<UUID, FilteredPlayer> players = new ConcurrentHashMap<>();
    private final OkHttpClient client = new OkHttpClient();
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<Message> messageJsonAdapter = moshi.adapter(Message.class);
    private final JsonAdapter<BackendResponse> backendResponseJsonAdapter = moshi.adapter(BackendResponse.class);
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final AtomicInteger backendCounter = new AtomicInteger();

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(PistonChatEvent event) {
        handleMessage(event.getPlayer(), event.getMessage(),
                () -> event.setCancelled(true),
                message -> event.setMessage(message),
                message -> CommonTool.sendChatMessage(event.getPlayer(), message, event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhisper(PistonWhisperEvent event) {
        if (event.getSender() == event.getReceiver()) return;

        handleMessage(event.getSender(), event.getMessage(),
                () -> event.setCancelled(true),
                message -> event.setMessage(message),
                message -> CommonTool.sendSender(event.getSender(), message, event.getReceiver()));
    }

    public void handleMessage(CommandSender sender, String message, Runnable cancelEvent, Consumer<String> modifyMessage, Consumer<String> sendEmpty) {
        Instant now = Instant.now();
        if (sender.hasPermission("pistonfilter.bypass")) return;

        if (plugin.getConfig().getBoolean("backend-processing.enable")) {
            if (plugin.getConfig().getString("backend-processing.strategy").equals("before")) {
                BackendResponse response = backendProcess(sender, message, 0);
                if (response.replacement != null) {
                    modifyMessage.accept(response.replacement);
                } // No else in case of "message-sender" is true
                if (!response.allowed) {
                    cancelMessage(sender, message, cancelEvent, sendEmpty);
                    return;
                }
            } else if (plugin.getConfig().getString("backend-processing.strategy").equals("exclusive")) {
                BackendResponse response = backendProcess(sender, message, 0);
                if (response.replacement != null)
                    modifyMessage.accept(response.replacement);
                if (!response.allowed)
                    cancelMessage(sender, message, cancelEvent, sendEmpty);
                return;
            }
        }

        String[] words = Arrays.stream(message.split(" ")).filter(word -> !word.isEmpty()).toArray(String[]::new);
        String cutMessage = message.toLowerCase().replace(" ", "").replaceAll("\\s+", "");

        for (String str : plugin.getConfig().getStringList("banned-text")) {
            if (Pattern.compile(StringHelper.toLeetPattern(str).toLowerCase()).matcher(cutMessage).find()) {
                cancelMessage(sender, message, cancelEvent, sendEmpty);
                return;
            }
        }

        int wordsWithNumbers = 0;
        for (String word : words) {
            if (word.length() > plugin.getConfig().getInt("max-word-length")
                    || hasInvalidSeparators(word)) {
                cancelMessage(sender, message, cancelEvent, sendEmpty);
                return;
            }

            if (StringHelper.containsDigit(word)) {
                wordsWithNumbers++;
            }
        }

        if (wordsWithNumbers > plugin.getConfig().getInt("max-words-with-numbers")) {
            cancelMessage(sender, message, cancelEvent, sendEmpty);
            return;
        }

        if (plugin.getConfig().getBoolean("norepeat")) {
            boolean blocked = false;
            UUID uuid = new UniqueSender(sender).getUniqueId();
            FilteredPlayer filteredPlayerCached = players.get(uuid);

            if (filteredPlayerCached != null) {
                Pair<Instant, String> lastMessage = filteredPlayerCached.getLastMessage();

                if (lastMessage != null) {
                    Instant lastMessageTime = lastMessage.getKey();
                    String lastMessageText = lastMessage.getValue();

                    if (Duration.between(lastMessageTime, now).getSeconds() < plugin.getConfig().getInt("norepeat-time")
                        || FuzzySearch.ratio(lastMessageText, cutMessage) > plugin.getConfig().getInt("similarration")) {
                        blocked = true;
                        cancelMessage(sender, message, cancelEvent, sendEmpty);
                    }
                }
            }

            if (!blocked) {
                if (filteredPlayerCached == null) {
                    filteredPlayerCached = new FilteredPlayer(new UniqueSender(sender).getUniqueId());
                    players.put(uuid, filteredPlayerCached);
                }

                filteredPlayerCached.setLastMessage(new Pair<>(Instant.now(), cutMessage));
            }
        }

        if (plugin.getConfig().getBoolean("backend-processing.enable")
                && plugin.getConfig().getString("backend-processing.strategy").equals("after")) {
            BackendResponse response = backendProcess(sender, message, 0);
            if (response.replacement != null)
                modifyMessage.accept(response.replacement);
            if (!response.allowed)
                cancelMessage(sender, message, cancelEvent, sendEmpty);
        }
    }

    private boolean hasInvalidSeparators(String word) {
        List<Character> chars = word.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        int maxSeparators = plugin.getConfig().getInt("max-separated-numbers");

        int separators = 0;
        int index = 0;
        for (char c : chars) {
            if (Character.isDigit(c)) {
                if (index >= (chars.size() - 1)) {
                    return false;
                } else {
                    if (!Character.isDigit(chars.get(index + 1))) {
                        separators++;
                        if (separators > maxSeparators) {
                            return true;
                        }
                    }
                }
            }
            index++;
        }
        return false;
    }

    private BackendResponse backendProcess(CommandSender sender, String message, int retries) {
        BackendResponse responseOnError = new BackendResponse(
                plugin.getConfig().getBoolean("backend-processing.allow-on-error"),
                null
        );
        int timeout = plugin.getConfig().getInt("backend-processing.timeout");

        if (retries > plugin.getConfig().getInt("backend-processing.retries"))
            return responseOnError;

        List<String> servers = plugin.getConfig().getStringList("backend-processing.servers");
        RequestBody body = RequestBody.create(messageJsonAdapter.toJson(new Message(sender.getName(), message)), JSON);
        Request request = new Request.Builder()
                .url(servers.get(backendCounter.incrementAndGet() % servers.size())) // % and atomics are kind of slow, there is a room for improvement there
                .post(body)
                .build();

        OkHttpClient dirtyClient = client.newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();

        try (Response res = dirtyClient.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                plugin.getLogger().severe(ChatColor.RED + "[AntiSpam] Request to a backend processor failed");
                return backendProcess(sender, message, retries + 1);
            }
            return backendResponseJsonAdapter.fromJson(res.body().source());
        } catch (IOException e) {
            plugin.getLogger().severe(ChatColor.RED + "[AntiSpam] Request to a backend processor failed");
            return backendProcess(sender, message, retries + 1);
        }
    }

    private void cancelMessage(CommandSender sender, String message, Runnable cancelEvent, Consumer<String> sendEmpty) {
        cancelEvent.run();

        if (plugin.getConfig().getBoolean("message-sender")) {
            sendEmpty.accept(message);
        }

        if (plugin.getConfig().getBoolean("verbose")) {
            plugin.getLogger().info(ChatColor.RED + "[AntiSpam] <" + sender.getName() + "> " + message);
        }
    }
}
