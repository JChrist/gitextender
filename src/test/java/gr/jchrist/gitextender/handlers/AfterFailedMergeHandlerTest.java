package gr.jchrist.gitextender.handlers;

import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class AfterFailedMergeHandlerTest {
    @Test
    public void afterMerge(final @Mocked MergeState mergeState) throws Exception {
        new AfterFailedMergeHandler(mergeState).afterMerge();

        new Verifications() {{
            mergeState.getLocalHistoryAction().finish();
        }};
    }
}