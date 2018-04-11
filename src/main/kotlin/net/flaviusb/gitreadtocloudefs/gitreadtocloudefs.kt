package net.flaviusb.gitreadtocloudefs

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;


class GitReadToCloudEFS {

  /*
  Open repo.
  For each changeset in repo, get all parents.
  Topological sort.

  Map between commits and cloudEFS hashes

  For each changeset in the sorted set
    Get the commit hash.
    change to associated parent in CloudEFS, if there is a parent.
    Delete all files, then copy over all files.
    Commit, and store the hash in the commit -> cloudEFS Map.

  */
  fun stuff() {
    // Open repo. We assume the cwd is in the git directory we are to stream from.
    var repo = FileRepositoryBuilder().readEnvironment().findGitDir().build();
  }

}
