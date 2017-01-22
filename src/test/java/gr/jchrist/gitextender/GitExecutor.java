/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gr.jchrist.gitextender;

import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitExecutor {

    protected static final Logger LOG = Logger.getInstance(GitExecutor.class);

    public static class ExecutionException extends RuntimeException {

        private final int myExitCode;
        @NotNull private final String myOutput;

        ExecutionException(int exitCode, @NotNull String output) {
            super("Failed with exit code " + exitCode);
            myExitCode = exitCode;
            myOutput = output;
        }

        public int getExitCode() {
            return myExitCode;
        }

        @NotNull
        public String getOutput() {
            return myOutput;
        }
    }

    private static String ourCurrentDir;

    private static void cdAbs(@NotNull String absolutePath) {
        ourCurrentDir = absolutePath;
        debug("# cd " + shortenPath(absolutePath));
    }

    public static void debug(@NotNull String msg) {
        if (!StringUtil.isEmptyOrSpaces(msg)) {
            LOG.info(msg);
        }
    }

    private static void cdRel(@NotNull String relativePath) {
        cdAbs(ourCurrentDir + "/" + relativePath);
    }

    public static void cd(@NotNull String relativeOrAbsolutePath) {
        if (relativeOrAbsolutePath.startsWith("/") || relativeOrAbsolutePath.charAt(1) == ':') {
            cdAbs(relativeOrAbsolutePath);
        }
        else {
            cdRel(relativeOrAbsolutePath);
        }
    }

    @NotNull
    public static File touch(String filePath) {
        try {
            File file = child(filePath);
            assert !file.exists() : "File " + file + " shouldn't exist yet";
            //noinspection ResultOfMethodCallIgnored
            new File(file.getParent()).mkdirs(); // ensure to create the directories
            boolean fileCreated = file.createNewFile();
            assert fileCreated;
            debug("# touch " + filePath);
            return file;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static File touch(@NotNull String fileName, @NotNull String content) {
        File filePath = touch(fileName);
        echo(fileName, content);
        return filePath;
    }

    public static void echo(@NotNull String fileName, @NotNull String content) {
        try {
            FileUtil.writeToFile(child(fileName), content.getBytes(), true);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void append(@NotNull File file, @NotNull String content) throws IOException {
        FileUtil.writeToFile(file, content.getBytes(), true);
    }

    public static void append(@NotNull String fileName, @NotNull String content) throws IOException {
        append(child(fileName), content);
    }

    @NotNull
    protected static String run(@NotNull File workingDir, @NotNull List<String> params, boolean ignoreNonZeroExitCode)
            throws ExecutionException
    {
        final ProcessBuilder builder = new ProcessBuilder().command(params);
        builder.directory(workingDir);
        builder.redirectErrorStream(true);
        Process clientProcess;
        try {
            clientProcess = builder.start();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        String commandLine = StringUtil.join(params, " ");
        CapturingProcessHandler handler = new CapturingProcessHandler(clientProcess, CharsetToolkit.getDefaultSystemCharset(), commandLine);
        ProcessOutput result = handler.runProcess(30 * 1000);
        if (result.isTimeout()) {
            throw new RuntimeException("Timeout waiting for the command execution. Command: " + commandLine);
        }

        String stdout = result.getStdout().trim();
        if (result.getExitCode() != 0) {
            if (ignoreNonZeroExitCode) {
                debug("{" + result.getExitCode() + "}");
            }
            debug(stdout);
            if (!ignoreNonZeroExitCode) {
                throw new ExecutionException(result.getExitCode(), stdout);
            }
        }
        else {
            debug(stdout);
        }
        return stdout;
    }

    @NotNull
    public static List<String> splitCommandInParameters(@NotNull String command) {
        List<String> split = new ArrayList<>();

        boolean insideParam = false;
        StringBuilder currentParam = new StringBuilder();
        for (char c : command.toCharArray()) {
            boolean flush = false;
            if (insideParam) {
                if (c == '\'') {
                    insideParam = false;
                    flush = true;
                }
                else {
                    currentParam.append(c);
                }
            }
            else if (c == '\'') {
                insideParam = true;
            }
            else if (c == ' ') {
                flush = true;
            }
            else {
                currentParam.append(c);
            }

            if (flush) {
                if (!StringUtil.isEmptyOrSpaces(currentParam.toString())) {
                    split.add(currentParam.toString());
                }
                currentParam = new StringBuilder();
            }
        }

        // last flush
        if (!StringUtil.isEmptyOrSpaces(currentParam.toString())) {
            split.add(currentParam.toString());
        }
        return split;
    }

    @NotNull
    private static String shortenPath(@NotNull String path) {
        String[] split = path.split("/");
        if (split.length > 3) {
            // split[0] is empty, because the path starts from /
            return String.format("/%s/.../%s/%s", split[1], split[split.length - 2], split[split.length - 1]);
        }
        return path;
    }

    @NotNull
    protected static File child(@NotNull String fileName) {
        assert ourCurrentDir != null : "Current dir hasn't been initialized yet. Call cd at least once before any other command.";
        return new File(ourCurrentDir, fileName);
    }

    @NotNull
    protected static File ourCurrentDir() {
        assert ourCurrentDir != null : "Current dir hasn't been initialized yet. Call cd at least once before any other command.";
        return new File(ourCurrentDir);
    }

    private static final int MAX_RETRIES = 3;
    private static boolean myVersionPrinted;

    //using inner class to avoid extra work during class loading of unrelated tests
    public static class PathHolder {
        public static final String GIT_EXECUTABLE = "git";
    }

    public static String git(String command) {
        return git(command, false);
    }

    public static String git(String command, boolean ignoreNonZeroExitCode) {
        printVersionTheFirstTime();
        return doCallGit(command, ignoreNonZeroExitCode);
    }

    @NotNull
    private static String doCallGit(String command, boolean ignoreNonZeroExitCode) {
        List<String> split = splitCommandInParameters(command);
        split.add(0, PathHolder.GIT_EXECUTABLE);
        File workingDir = ourCurrentDir();
        debug("[" + workingDir.getName() + "] # git " + command);
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String stdout;
            try {
                stdout = run(workingDir, split, ignoreNonZeroExitCode);
                if (!isIndexLockFileError(stdout)) {
                    return stdout;
                }
            }
            catch (ExecutionException e) {
                stdout = e.getOutput();
                if (!isIndexLockFileError(stdout)) {
                    throw e;
                }
            }
            LOG.info("Index lock file error, attempt #" + attempt + ": " + stdout);
        }
        throw new RuntimeException("fatal error during execution of Git command: $command");
    }

    private static boolean isIndexLockFileError(@NotNull String stdout) {
        return stdout.contains("fatal") && stdout.contains("Unable to create") && stdout.contains(".git/index.lock");
    }

    public static void add() {
        add(".");
    }

    public static void add(@NotNull String path) {
        git("add --verbose " + path);
    }

    @NotNull
    public static String addCommit(@NotNull String message) {
        add();
        return commit(message);
    }

    public static void checkout(@NotNull String... params) {
        git("checkout " + StringUtil.join(params, " "));
    }

    public static String commit(@NotNull String message) {
        git("commit -m '" + message + "'");
        return last();
    }

    @NotNull
    public static String tac(@NotNull String file, @NotNull String content) {
        touch(file, content);
        return addCommit("touched " + file);
    }

    @NotNull
    public static String tac(@NotNull String file) {
        touch(file, "content" + Math.random());
        return addCommit("touched " + file);
    }

    @NotNull
    public static String push() {
        return git("push");
    }

    @NotNull
    public static String push(String remoteName, String branch) {
        return git("push -u " + remoteName+" "+branch);
    }

    @NotNull
    public static String last() {
        return git("log -1 --pretty=%H");
    }

    private static void printVersionTheFirstTime() {
        if (!myVersionPrinted) {
            myVersionPrinted = true;
            doCallGit("version", false);
        }
    }
}
