import com.mifmif.common.regex.Generex;
import net.pistonmaster.pistonfilter.listeners.ChatListener;
import net.pistonmaster.pistonfilter.utils.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LeetTest {
    @Test
    void leetTest() {
        Assertions.assertEquals(16, new Generex(StringHelper.toLeetPattern("test12345-/(")).getAllMatchedStrings().size());
    }
}
