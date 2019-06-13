package gr.jchrist.gitextender.configuration;

public class GitExtenderSettings {
    private boolean attemptMergeAbort;
    private boolean pruneLocals;

    public GitExtenderSettings() {
        this(false, false);

    }

    public GitExtenderSettings(boolean attemptMergeAbort, boolean pruneLocals) {
        this.attemptMergeAbort = attemptMergeAbort;
        this.pruneLocals = pruneLocals;
    }

    public boolean getAttemptMergeAbort() {
        return attemptMergeAbort;
    }

    public void setAttemptMergeAbort(boolean attemptMergeAbort) {
        this.attemptMergeAbort = attemptMergeAbort;
    }

    public boolean getPruneLocals() {
        return pruneLocals;
    }

    public void setPruneLocals(boolean pruneLocals) {
        this.pruneLocals = pruneLocals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitExtenderSettings settings = (GitExtenderSettings) o;

        return attemptMergeAbort == settings.attemptMergeAbort && pruneLocals == settings.pruneLocals;
    }

    @Override
    public int hashCode() {
        return (attemptMergeAbort ? 1 : 0) + (pruneLocals ? 10 : 0);
    }

    @Override
    public String toString() {
        return "GitExtenderSettings{" +
                "attemptMergeAbort=" + attemptMergeAbort +
                "pruneLocals=" + pruneLocals +
                '}';
    }
}
