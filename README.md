# Git Extender
Git Extender is a plugin for IntelliJ products, 
which allows updating all local branches of all modules in the current project.

It will update all local branches of all git roots in the current project.

Local branches that will be updated are the branches that exist locally and have been configured
to track a remote branch.

It *always* fast-forwards commits in remote branches to local branches. If the local branch cannot be merged to the
tracked remote using fast-forward only, then it will not be updated and an error notification will be shown. In this
case, the update must be performed manually for the reported branch

After updating a branch, if there were any file changes, they will be displayed in IntelliJ Version Control tab.
Currently, the correct list of file changes (updated, created, removed) will be displayed. However, when performing
a diff for files in a branch other than the currently checked out, the diff will most probably be incorrect 