Changelog
---------

## 0.5.2 (2019-09-26)
* Fixed missing backgroundable task spinning notification and disabled concurrent executions

## 0.5.1 (2019-09-25)
* Updated threading model for updating repositories, (hopefully) closing issue #13
* Refactored all tests to not use the hacky way of BaseTest with Inner platform test case, instead extending BasePlatformTestCase or AbstractIT (which in turn extends HeavyPlatformTestCase)

## 0.5.0 (2019-06-13)
* Added _prune local branches_ feature, so that a local branch gets deleted if it was tracking a remote one prior to fetching (and pruning) and its remote counter-part was pruned

## 0.4.1 (2017-12-06)
- Fix issue #8: create UpdateInfoTree in the event dispatch thread 

## 0.4.0 (2017-07-01)
- For projects with multiple modules as separate Git roots, a dialog will open up, offering the option to select which module(s) to update. 
- Refactored persisted configuration settings.

## 0.3.1 (2017-03-23)
- Removed until version in plugin.xml, so that it is compatible with latest releases

## 0.3.0 (2017-01-23)
- Moved plugin build to gradle and added !**tests**, as well as Travis CI builds.
- Added settings page, for controlling whether merge-abort-on-error will be attempted
- Added Apache License Version 2.0 reference.

## 0.2.0 (2016-04-02)
- Keeping track of updated files, as results to be displayed after updating

## 0.1.3 (2016-02-13)
- Changed pull method from rebase to merge, with the --ff-only flag (fast-forward only)

## 0.1.2 (2015-07-17)
- Using GitFetcher to fetch and prune remotes and show results before starting the update 

## 0.1.1 (2015-06-29)
- Changed jdk to 1.7, so that the plugin works with IDEA running with 1.7 jdk
- Added check if any staged or unstaged changes exist in each repository, in order to stash only if needed

## 0.1 (2015-06-28)
- Initial version of Git Extender, with support for updating all local branches tracking a remote of all git roots
in the current project, using rebase for all local commits and automatic stashing/popping of uncommitted changes
