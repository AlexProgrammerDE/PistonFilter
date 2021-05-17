import com.mifmif.common.regex.Generex;
import net.pistonmaster.pistonfilter.ChatListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LeetTest {
    @Test
    void leetTest() {
        Assertions.assertEquals(16, new Generex(ChatListener.toLeetPattern("test12345-/(")).getAllMatchedStrings().size());
    }
}
