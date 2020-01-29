package skinsrestorer.shared.update;

import org.inventivetalent.update.spiget.ResourceInfo;

import java.util.List;

/**
 * Created by McLive on 11.04.2019.
 */
public class GitHubReleaseInfo extends ResourceInfo {
    public String tag_name;
    public String name;
    public List<GitHubAssetInfo> assets;

    public String latestDownloadURL;

    public GitHubReleaseInfo() {
    }
}
