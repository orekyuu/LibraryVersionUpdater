# Library Version Updater
build.gradleのライブラリを更新してGitHubにPullRequestを作成します

## Plugin Page
[LibraryVersionUpdaterPlugin](https://plugins.gradle.org/plugin/net.orekyuu.library-version-updater.LibraryVersionUpdaterPlugin)

## 準備
https://github.com/settings/tokens/new?description=LibraryVersionUpdater%20token で `repo` にチェックを入れてアクセストークンを作成してください

## 設定例
```build.gradle
createLibraryUpdatePR {
    githubAccessToken = getProperty("github.token")
    githubPage = "https://github.com/orekyuu/LibraryVersionUpdater"
    basedBranchName = "master"
}

// 下の設定を書かないとbetaなどにも更新される
dependencyUpdates.resolutionStrategy = {
    componentSelection { rules ->
        rules.all { ComponentSelection selection ->
            boolean rejected = ['alpha', 'beta', 'rc', 'cr', 'm'].any { qualifier ->
                selection.candidate.version ==~ /(?i).*[.-]${qualifier}[.\d-]*/
            }
            if (rejected) {
                selection.reject('Release candidate')
            }
        }
    }
}
```
