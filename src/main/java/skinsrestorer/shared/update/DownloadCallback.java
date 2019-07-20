package skinsrestorer.shared.update;

/**
 * Created by McLive on 13.04.2019.
 */
public interface DownloadCallback {
    void finished();

    void error(Exception e);
}
