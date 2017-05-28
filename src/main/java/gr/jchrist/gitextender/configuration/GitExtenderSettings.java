package gr.jchrist.gitextender.configuration;

public class GitExtenderSettings {
    private boolean attemptMergeAbort;

    public GitExtenderSettings() {
        this(false);

    }

    public GitExtenderSettings(boolean attemptMergeAbort) {
        this.attemptMergeAbort = attemptMergeAbort;
    }

    public boolean getAttemptMergeAbort() {
        return attemptMergeAbort;
    }

    public void setAttemptMergeAbort(boolean attemptMergeAbort) {
        this.attemptMergeAbort = attemptMergeAbort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitExtenderSettings settings = (GitExtenderSettings) o;

        return attemptMergeAbort == settings.attemptMergeAbort;
    }

    @Override
    public int hashCode() {
        return (attemptMergeAbort ? 1 : 0);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GitExtenderSettings{");
        sb.append("attemptMergeAbort=").append(attemptMergeAbort);
        sb.append('}');
        return sb.toString();
    }
}
