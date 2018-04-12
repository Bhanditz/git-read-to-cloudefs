package net.flaviusb.gitreadtocloudefs

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;

import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.fejoa.AccountIO
import org.fejoa.FejoaContext
import org.fejoa.UserData
import org.fejoa.crypto.CryptoSettings
import org.fejoa.crypto.generateSecretKeyData
import org.fejoa.fs.fusejnr.FejoaFuse
import org.fejoa.fs.fusejnr.Utils
import org.fejoa.storage.StorageDir
import org.fejoa.support.StorageLib
import org.fejoa.support.StreamHelper
import org.fejoa.support.toInStream

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
    var parent: MutableMap<String, MutableSet<String>> = mutableMapOf()
    var child: MutableMap<String, MutableSet<String>> = mutableMapOf()
    var all_commits: MutableList<String> = mutableListOf()
    for (commit in revWalk) {
      val own_hash = ObjectId.toString(commit.getId())
      all_commits.add(own_hash)
      val parents: Array<RevCommit> = commit.getParents().clone()
      val parent_hashes: MutableSet<String> = parents.map({c: RevCommit -> ObjectId.toString(c.getId()) }).toMutableSet()
      parent.put(own_hash, parent_hashes)
      for(p in parent_hashes) {
        var c = child.get(p)
        if(c != null) {
          c.add(own_hash)
          child.put(p, c)
        } else {
          child.put(p, mutableSetOf(own_hash))
        }
      }
    }
    val sorterater = sortify(child, parent);
    all_commits.sortWith(sorterater);
    // We use an array for the commit order because that provides us an implicit foreign key (the index) for motching with the cloudEFS version hash
    val commit_order: Array<String> = all_commits.toTypedArray();
    //var cloudEfsHash: Array<...?> = arrayOfNulls(all_commits.size)
  }

  fun sortify(lt: Map<String, Set<String>>, gt: Map<String, Set<String>>): Comparator<String> {
    val foo : Set<String> = arrayOf<String>().toSet();
    return object : Comparator<String> {
      override fun compare(l: String, r: String): Int = if (lt.getOrDefault(l, foo).contains(r)) { -1 } else if (gt.getOrDefault(l, foo).contains(r)) { +1 } else { 0 }
    }
  }
}
