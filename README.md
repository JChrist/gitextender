# gitextender
Git Extender is a plugin for IntelliJ products, 
which allows updating all local branches of all modules in the current project.

It will update all local branches of all git roots in the current project.

Local branches that will be updated are the branches that exist locally and have been configured
to track a remote branch.

It *always* fast-forwards commits in remote branches to local branches. If the local branch cannot be merged to the
tracked remote using fast-forward only, then it will not be updated and an error notification will be shown. In this
case, the update must be performed manually for the reported branch
