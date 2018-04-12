package net.flaviusb.gitreadtocloudefs

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevWalk;

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
    var refs = repo.getAllRefs();
    var revWalk = RevWalk(repo);
    for((key, ref) in refs) {
      revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
    }
    // So, we create two maps here; a hash -> Set(child hashes), and a hash -> Set(parent hashes)
    // What we want is a sorted list of hashes, where anything wh
    for (commit in revWalk) {
    }
  }

  fun sort(lt: Map<String, Set<String>>, gt: Map<String, Set<String>>): Comparator<String> {
    val foo : Set<String> = arrayOf<String>().toSet();
    return object : Comparator<String> {
      override fun compare(l: String, r: String): Int = if (lt.getOrDefault(l, foo).contains(r)) { -1 } else if (gt.getOrDefault(l, foo).contains(r)) { +1 } else { 0 }
    }
  }
}
