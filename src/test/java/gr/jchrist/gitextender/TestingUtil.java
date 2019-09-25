package gr.jchrist.gitextender;

import git4idea.commands.GitCommandResult;

import java.util.Collections;

public abstract class TestingUtil {
    public static final GitCommandResult error =
            new GitCommandResult(true, 1,
                    Collections.singletonList("test error output"),
                    Collections.singletonList("error"));
    public static final GitCommandResult success =
            new GitCommandResult(false, 0,
                    Collections.emptyList(),
                    Collections.singletonList("success"));
}
