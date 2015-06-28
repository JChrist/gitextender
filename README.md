# gitextender
Git Extender is a plugin for IntelliJ products, 
which allows updating all local branches of all modules in the current project.

It will update all local branches of all git roots in the current project.

Local branches that will be updated are the branches that exist locally and have been configured
to track a remote branch.

It *always* rebases commits in local branches on top of the commits that are pulled from the remote branch
