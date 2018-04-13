package net.flaviusb.gitreadtocloudefs

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

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
import org.fejoa.crypto.*
import org.fejoa.repository.*
import org.fejoa.storage.*
import org.fejoa.support.*
import org.fejoa.chunkcontainer.*

fun main(args: Array<String>) {
  val thing = GitReadToCloudEFS()
  thing.stuff()
}
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
    // We use an array for the commit order because that provides us an implicit foreign key (the index) for matching with the cloudEFS version hash
    val commit_order: Array<String> = all_commits.toTypedArray();
    val index_commit_order: Map<String, Int> = mapOf<String, Int>(*(commit_order.mapIndexed({a: Int, b: String -> b to a}).toTypedArray()));
    var cloudEfsHash: Array<org.fejoa.storage.Hash?> = arrayOfNulls(all_commits.size)
    // Set up the cloudEFS repository
    // Lots of copy-paste from fejoa test files
    secretKey = runBlocking { CryptoHelper.crypto.generateSymmetricKey(settings.symmetric) }
    storageBackend = platformCreateStorage("")

    val repo_efs = runBlocking { createRepo("cloudEFSbenchmark", "fromGit") }

    // For each changeset in the sorted set...
    val emptySet: MutableSet<String> = mutableSetOf<String>()
    for((index, commit) in commit_order.withIndex()) {
      // Set the current branch to the first parent branch, if we have a parent for the current commit
      val par = parent.getOrDefault(commit, emptySet);
      if (par.size > 0) {
        // Choose the first parent
        val parent_index_str = par.toTypedArray().get(0)
        if(parent_index_str != null) {
          val thing = cloudEfsHash[index_commit_order.getOrDefault(parent_index_str, -1)]
          if (thing != null) {
            runBlocking { repo_efs.setHeadCommit(thing) }
          }
        }
      } else {
        
      }
      // First, delete everything
      val file_listing = runBlocking { repo_efs.listFiles("/") }
      for (file in file_listing) {
        runBlocking { repo_efs.remove(file) }
      }
      // For each file in the git commit, add it to the current cloudefs commit
      val rev_commit = RevWalk(repo).parseCommit(ObjectId.fromString(commit))
      val file_tree = rev_commit.getTree()
      var tree_walk = TreeWalk(repo)
      tree_walk.addTree(file_tree)
      tree_walk.setRecursive(false)
      tree_walk.setPostOrderTraversal(false)
      while(tree_walk.next()) {
        val filename = tree_walk.getPathString()
        val loader = repo.open(tree_walk.getObjectId(0))
        runBlocking { repo_efs.putBytes(filename, loader.getBytes()) }
      }
      val my_hash = runBlocking { repo_efs.commit(rev_commit.getFullMessage().toByteArray(), null) }
      //val my_hash = runBlocking {repo_efs.getHeadCommit()?.getHash()};
      cloudEfsHash[index] = my_hash;
    }
  }

  fun sortify(lt: Map<String, Set<String>>, gt: Map<String, Set<String>>): Comparator<String> {
    val foo : Set<String> = arrayOf<String>().toSet();
    return object : Comparator<String> {
      override fun compare(l: String, r: String): Int = if (lt.getOrDefault(l, foo).contains(r)) { -1 } else if (gt.getOrDefault(l, foo).contains(r)) { +1 } else { 0 }
    }
  }

  // Helper functions taken from ChunkContainerTestBase.kt
  protected var settings = CryptoSettings.default
  protected var secretKey: SecretKey? = null
  protected var storageBackend: StorageBackend? = null
  protected suspend fun prepareStorage(dirName: String, name: String): StorageBackend.BranchBackend {
    return storageBackend?.let {
        val branchBackend = if (it.exists(dirName, name))
            it.open(dirName, name)
        else {
            it.create(dirName, name)
        }
        //cleanUpList.add(dirName)
        return@let branchBackend
    } ?: throw Exception("storageBackend should not be null")
  }
  protected fun getRepoConfig(): RepositoryConfig {
    val seed = ByteArray(10) // just some zeros
    val hashSpec = HashSpec.createCyclicPoly(HashSpec.HashType.FEJOA_CYCLIC_POLY_2KB_8KB, seed)

    val boxSpec = BoxSpec(
            encInfo = BoxSpec.EncryptionInfo(BoxSpec.EncryptionInfo.Type.PARENT),
            zipType = BoxSpec.ZipType.DEFLATE,
            zipBeforeEnc = true
    )

    return RepositoryConfig(
            hashSpec = hashSpec,
            boxSpec = boxSpec
    )
  }
  // Helper functions taken from RepositoryTestBase.kt
  protected suspend fun createRepo(dirName: String, branch: String, init: Boolean = true): org.fejoa.repository.Repository {
    val storage = prepareStorage(dirName, branch)
    return org.fejoa.repository.Repository.create(branch, storage, getRepoConfig(),
            SecretKeyData(secretKey!!, settings.symmetric.algo), init)
  }

}
