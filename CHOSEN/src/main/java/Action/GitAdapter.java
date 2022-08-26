package Action;

import Obj.CommitMessage;
import com.gitblit.models.PathModel;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.*;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author swf
 */

public class GitAdapter {
    private static final Logger logger = LoggerFactory.getLogger(GitAdapter.class);

    private final static String REF_HEADS = "refs/heads/";
    private final static String MASTER_BRANCH = "master";
    // 远程仓库路径  用的就是.git
    private final String remotePath;

    // 本地仓库路径  包含了项目工程名projectName
    private String localPath;

    private String localGitPath;

    private Git git;

    private Repository repository;

    public String branchName;
    //  Git授权
    private static UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider;

    /**
     * 构造函数：没有传分支信息则默认拉取master代码
     *
     * @param remotePath 远程仓库的位置
     * @param localPath  本地仓库的位置
     */
    public GitAdapter(String remotePath, String localPath) {
        this(remotePath, localPath, MASTER_BRANCH);
    }

    public GitAdapter(String remotePath, String localPath, String branchName) {
        this.remotePath = remotePath;
        this.localPath = localPath;
        this.branchName = branchName;
        localGitPath = this.localPath + "/.git";
        // 鉴权账户密码可用自己gitHub的账户密码，或者是设置token
        this.usernamePasswordCredentialsProvider = new UsernamePasswordCredentialsProvider("account", "password");
    }

    /**
     * 使用Git时需要先初始化git
     * 默认初始化的时候会自动拉取 @branchName 的最新代码
     *
     * @return 返回Git
     */
    public Git initGit() {
        File file = new File(localPath);
        // 如果文件存在 说明已经拉取过代码,则拉取最新代码
        if (file.exists()) {
            try {
                git = Git.open(new File(localPath));
                // 判断是否是最新代码 判断是否是最新代码好像耗时更久？？！  暂时更新远程仓库，只保留现阶段的仓库文件
//                boolean isLatest = checkBranchNewVersion(git.getRepository().exactRef(REF_HEADS+branchName));
//                if (isLatest==true) {
//                    logger.info("the local version is latest, need not pull it");
//                } else {
//                    // 拉取最新的提交
//                    git.pull().setCredentialsProvider(usernamePasswordCredentialsProvider).call();
//                    logger.info("pull success");
//                }
            }
//            catch (GitAPIException e) {
//                logger.info("pull failure");
//                e.printStackTrace();
//            }
            catch (IOException e) {
                logger.info("pull failure");
                e.printStackTrace();
            }
        }
        // 文件不存在，说明未拉取过代码 则拉取最新代码
        else {
            try {
                git = Git.cloneRepository()
                        .setCredentialsProvider(usernamePasswordCredentialsProvider)
                        .setURI(remotePath)
                        .setBranch(branchName)
                        .setDirectory(new File(localPath))
                        .call();
                // 拉取最新的提交
                git.pull().setCredentialsProvider(usernamePasswordCredentialsProvider).call();
                logger.info("down success");
            } catch (GitAPIException e) {
                logger.error("远程仓库下载异常");
                e.printStackTrace();
            }
        }
        repository = git.getRepository();
        return git;
    }

    /**
     * 获取ref信息
     *
     * @return 返回当前分支的Ref
     * @throws IOException
     */
    public Ref getBranchRef() throws IOException {
        return getBranchRef(this.branchName);
    }

    /**
     * 根据branch 获取ref信息
     *
     * @param branchName 分支名称
     * @return 返回branch下的ref信息
     */
    public Ref getBranchRef(String branchName) {
        try {
            return git.getRepository().exactRef(REF_HEADS + branchName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定分支的指定文件内容
     *
     * @param branchName 分支名称
     * @param javaPath   文件路径
     * @return java类
     * @throws IOException
     */
    public String getBranchSpecificFileContent(String branchName, String javaPath) throws IOException {
        Ref branch =this.getBranchRef(branchName);
        ObjectId objId = branch.getObjectId();
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(objId);
        return getFileContent(javaPath, tree, walk, "UTF-8");
    }

    /**
     * 根据CommitName去获取文件内容
     *
     * @param Commitment Commit SHA1
     * @param javaPath   Java path
     * @return 文件内容
     */
     public String getCommitSpecificFileContent(String Commitment, String javaPath) throws IOException {

        RevWalk walk = new RevWalk(repository);  //获取仓库

        //获取的是具体的Commit的所属的文件内容
        var tree = walk.parseTree(walk.parseCommit(repository.resolve(Commitment)).getTree().getId());
        return getFileContent(javaPath, tree, walk, "UTF-8");
    }

    /**
     * 获取指定分支指定的指定文件内容
     *
     * @param javaPath 件路径
     * @param tree     git RevTree
     * @param walk     git RevWalk
     * @return java类
     * @throws IOException
     */
     private String getFileContent(String javaPath, RevTree tree, RevWalk walk, String charsets) throws IOException {

        TreeWalk treeWalk = TreeWalk.forPath(repository, javaPath, tree);
        if (treeWalk == null) {
            boolean flag = false;
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            ObjectReader reader = repository.newObjectReader();
            treeParser.reset(reader, tree.getId());
            ObjectReader objectReader = repository.newObjectReader();
            treeWalk = new TreeWalk(objectReader);
            treeWalk.addTree(treeParser);
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                if (treeWalk.getPathString().equals(javaPath)) {
                    flag = true;
                    break;
                }
            }
            assert flag; //assert that we can find the tree .
        }

        ObjectId blobId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(blobId, Constants.OBJ_BLOB);
        byte[] bytes = loader.getBytes();
        walk.dispose();
        return new String(bytes, charsets);
    }

    /**
     * 分析分支树结构信息
     *
     * @param localRef 本地分支
     * @return 解析树
     * @throws IOException
     */
    public AbstractTreeIterator prepareTreeParser(Ref localRef) throws IOException {
        return prepareTreeParser(localRef.getObjectId().getName());
    }

    /**
     * 通过CommitId获取分支树结构信息
     * 此方法是为了兼容
     * 被比较的分支（一般为master分支）的commitId已知的情况下，无需在获取Ref直接通过commitId获取分支树结构
     *
     * @param commitId Commit SHA1
     * @return 解析树
     * @throws IOException
     */
    public AbstractTreeIterator prepareTreeParser(String commitId) throws IOException {
        RevWalk walk = new RevWalk(repository);
        RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
        return prepareTreeParser(revCommit);
    }

    public AbstractTreeIterator prepareTreeParser(RevCommit revCommit) throws IOException {
        RevWalk walk = new RevWalk(repository);
        RevTree tree = walk.parseTree(revCommit.getTree().getId()); //解析树
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        treeParser.reset(reader, tree.getId());
        walk.dispose();
        return treeParser;
    }

    /**
     * 切换分支
     *
     * @param branchName 分支名称
     * @throws GitAPIException GitAPIException
     */
    public void checkOut(String branchName) throws GitAPIException {
        //  切换分支
        /*
        用于切换分支，其中@setCreateBranch用来确定是否要创建新的分支，同时可以通过setStartPoint设置分支的点；

         */
        git.checkout().setCreateBranch(false).setName(branchName).call();

    }

    /**
     * 切换分支 临时创建分支
     *
     * @param branchName 新的分支的名称
     * @param commitID   commitID
     * @throws GitAPIException GitAPIException
     */
    public void checkOut(String commitID, String branchName) throws GitAPIException {
        git.checkout().setCreateBranch(true).setName(branchName).setStartPoint(commitID).call(); //临时创建分支
    }


    /**
     * 更新分支代码
     *
     * @param branchName 分支名称
     * @throws GitAPIException GitAPIException
     */
    public Ref checkOutAndPull(String branchName) throws GitAPIException {
        // 1. 获取此分支的Ref信息
        Ref branchRef = getBranchRef(branchName);
        boolean isCreateBranch = branchRef == null;
        // 2. 如果Ref不为null，则校验Ref是否为最新，最新直接返回，不为最新则重新拉取分支信息
        if (!isCreateBranch && checkBranchNewVersion(branchRef)) {
            return branchRef;
        }
        //  3. 切换分支
        git.checkout().setCreateBranch(isCreateBranch).setName(branchName)
                .setStartPoint("origin/" + branchName)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call();
        //  4. 拉取最新代码
        git.pull().setCredentialsProvider(usernamePasswordCredentialsProvider).call();
        branchRef = getBranchRef(branchName);
        return branchRef;
    }


    /**
     * 切换到目标分支的版本中（暂时不用）
     *
     * @param versionCommit
     * @throws IOException
     * @throws GitAPIException
     */
    public void checkoutFromVersionCommit(String versionCommit) throws IOException, GitAPIException {
        RevWalk walk = new RevWalk(repository);
        ObjectId versionId = repository.resolve(versionCommit);
        RevCommit verCommit = walk.parseCommit(versionId);
        git.checkout().setCreateBranch(false).setName(versionCommit).setStartPoint(verCommit).call();
        git.branchDelete().setBranchNames(versionCommit);

//        Collection<Ref> remoteRefs = git.lsRemote().setHeads(true).setCredentialsProvider(usernamePasswordCredentialsProvider).call();
        String ref = git.getRepository().getRefDatabase().getRefs().toString();
        System.out.println(ref);
    }

    /**
     * 判断本地分支是否是最新版本。目前不考虑分支在远程仓库不存在，本地存在
     * 此方法有点耗时，可以考虑直接拉取最新版本
     *
     * @param localRef 本地分支
     * @return boolean 最新为True
     * @throws GitAPIException GitAPIException
     */
    private boolean checkBranchNewVersion(Ref localRef) throws GitAPIException {
        String localRefName = localRef.getName(); //  refs/heads/master 分支名
        String localRefObjectId = localRef.getObjectId().getName(); //commit_id
        //  获取远程所有分支
        Collection<Ref> remoteRefs = git.lsRemote().setCredentialsProvider(usernamePasswordCredentialsProvider).setHeads(true).call();
        for (Ref remoteRef : remoteRefs) {
            String remoteRefName = remoteRef.getName();
            String remoteRefObjectId = remoteRef.getObjectId().getName();
            if (remoteRefName.equals(localRefName)) {
                return remoteRefObjectId.equals(localRefObjectId);
            }
        }
        return false;
    }

    /**
     * 获取当前分支的前n个合并记录
     *
     * @return 前n个合并的提交记录
     * @throws IOException
     * @throws GitAPIException
     */
    public List<CommitMessage> getMergeCommitMessages(int n) throws IOException, GitAPIException {
        List<CommitMessage> commitMessages = new ArrayList<>();
        CommitMessage commitMessage;
        Iterable<RevCommit> commits = git.log().all().call();
        RevWalk walk = new RevWalk(repository);
        int flag = 0;
        for (RevCommit commit : commits) {
            commitMessage = new CommitMessage();
            boolean foundInThisBranch = false;
            RevCommit targetCommit = walk.parseCommit(commit.getId());
            for (Map.Entry<String, Ref> e : repository.getAllRefs().entrySet()) {
                if (e.getKey().startsWith("refs/remotes/origin")) {
                    if (walk.isMergedInto(targetCommit, walk.parseCommit(e.getValue().getObjectId()))) {
                        String foundInBranch = e.getValue().getTarget().getName();
//                        foundInBranch = foundInBranch.replace("refs/heads","");
                        if (foundInBranch.contains(branchName)) {
                            // 等于2 说明是来自两个合并的分支  算的是merge的记录
                            if (targetCommit.getParents().length == 2) {
                                flag++;
                                foundInThisBranch = true;
                                break;
                            }
                        }
                    }
                }

            }
            if (foundInThisBranch) {
                commitMessage.setCommitId(commit.getName());
                commitMessage.setCommitIdent(commit.getAuthorIdent().getName());
                commitMessage.setCommitMessage(commit.getFullMessage());
                commitMessage.setCommitDate(new Date(commit.getCommitTime() * 1000L).toString());
                commitMessage.setLastCommitId(commit.getParent(0).getName());
                commitMessage.setMergeBranchCommitId(commit.getParent(1).getName());
                commitMessages.add(commitMessage);
            }
            if (flag == n) {
                // 只取merge合并记录的前五条
                break;
            }

        }
        return commitMessages;
    }

    /**
     * 获取该仓库的所有的no-merge记录
     *
     * @return list:  返回的该仓库的所有非merge记录
     */
    public List<CommitMessage> getNo_MergeCommitMessages() throws IOException, GitAPIException {
        List<CommitMessage> commitMessages = new ArrayList<>();
        CommitMessage commitMessage;
        Iterable<RevCommit> commits = git.log().setRevFilter(RevFilter.NO_MERGES)
                .call();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (RevCommit commit : commits) {
            commitMessage = new CommitMessage();
            commitMessage.setCommitId(commit.getName());
            commitMessage.setCommitIdent(commit.getAuthorIdent().getName());
            commitMessage.setCommitMessage(commit.getFullMessage());
            commitMessage.setCommitDate(dateFormat.format(new Date(commit.getCommitTime() * 1000L)));
            if (commit.getParentCount() != 0) {
                commitMessage.setLastCommitId(commit.getParent(0).getName());
            }
            commitMessages.add(commitMessage);
        }
        return commitMessages;

    }

    /**
     * 实现git的过滤条件
     */
    public void grepPrintingResults(String revName, Pattern pattern) {
        ObjectReader objectReader = repository.newObjectReader();
        try {
            ObjectId commitID = repository.resolve(revName);
            impl(objectReader, commitID, pattern);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param pattern      查找的正则表示
     * @param objectReader repo中存储的Object数据库，从中获取流输出
     * @param commitID     Commit的ID值
     */

    private void impl(ObjectReader objectReader, ObjectId commitID, Pattern pattern) throws IOException {
        var treeWalk = new TreeWalk(objectReader);
        var revWalk = new RevWalk(objectReader);
        var commit = revWalk.parseCommit(commitID);
        var treeParser = new CanonicalTreeParser();
        treeParser.reset(objectReader, commit.getTree());
        int treeIndex = treeWalk.addTree(treeParser);
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            AbstractTreeIterator it = treeWalk.getTree(treeIndex,
                    AbstractTreeIterator.class);
            ObjectId objectId = it.getEntryObjectId();
            ObjectLoader objectLoader = objectReader.open(objectId);

            if (!isBinary(objectLoader.openStream())) {
                List<String> matchedLines = getMatchedLines(objectLoader.openStream(),pattern);
                if (!matchedLines.isEmpty()) {
                    String path = it.getEntryPathString();
                    for (String matchedLine : matchedLines) {
                        System.out.println(path + ":" + matchedLine);
                    }
                }
            }
        }
    }

    /**
     * 获取文件的内容，一句一句进行判断
     *
     * @param openStream:      文件的二进制流对象
     * @param pattern：要查找的正则表达
     * @return
     */
    private List<String> getMatchedLines(ObjectStream openStream, Pattern pattern) {
        BufferedReader buf;
        List<String> matchedLines = null;
        try {
            matchedLines = new ArrayList<>();
            buf = new BufferedReader(new InputStreamReader(openStream, StandardCharsets.UTF_8));
            String line;
            while ((line = buf.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    matchedLines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matchedLines;
    }

    /**
     * @param openStream 该对象打开了一个流对象，然后判断该对象是否为二进制
     * @return
     */
    private boolean isBinary(ObjectStream openStream) throws IOException {
        try {
            return RawText.isBinary(openStream);
        } finally {
            try {
                openStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 默认Java文件过滤
     * @param revName: Commit
     * @return Java文件
     */
    public List<String> getJavaFilesCommit(String revName){
        return this.getFilesCommit(revName, "^.*(.java)$");
    }

    /**
     * @param revName:CommitID的SHAI字符串
     * @param regrex : 过滤文件的模式
     * @return: 返回该revName下的总的tree（项目）的文件名称
     */
    public List<String> getFilesCommit(String revName, String regrex) {
        List<String> filesNames = null;
        try {
            filesNames = new ArrayList<>();
            ObjectId commitID = this.repository.resolve(revName);
            ObjectReader objectReader = repository.newObjectReader();

            TreeWalk treeWalk = new TreeWalk(objectReader);
            RevWalk revWalk = new RevWalk(objectReader);
            RevCommit commit = revWalk.parseCommit(commitID);
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(objectReader, commit.getTree());
            treeWalk.addTree(treeParser);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String line = treeWalk.getPathString();
                if (Pattern.matches(regrex, line)) //获取的是所有的Java文件
                {
                    filesNames.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filesNames;
      //  return null;
    }







//    /**
//     * 获取该提交的变化条目，只涉及到提交的文件列表内容，所提交的文件的修改类型；
//     * @param commitMessage 提交的CommitMessage
//     * @param isIgnore 是否忽略空白字符;
//     * @throws Exception
//     * @return Object[]: Object[0]格式器 and Object[1]获得前后版本的diffs
//     */
//    public Object[] get_DiffFiles(CommitMessage commitMessage, boolean isIgnore) throws Exception {
//
//        Git git = this.git;
//
//        Repository repository = git.getRepository();
//        if(commitMessage.getLastCommitId()==null||commitMessage.getLastCommitId().equals("")){
//            return null;
//        }
//
//        try (ObjectReader reader = repository.newObjectReader()) {
//            RevWalk wald = new RevWalk(reader);
//
//            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
//            newTreeIter.reset(reader, wald.parseCommit(repository.resolve(commitMessage.getCommitId())).getTree());
//            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
//            oldTreeIter.reset(reader, wald.parseCommit(repository.resolve(commitMessage.getLastCommitId())).getTree());
//            // finally get the list of changed files
//            List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
//
//            try (OutputStream tmpOutput = new ByteArrayOutputStream(2048)) {
//                DiffFormatter formatter = new DiffFormatter(tmpOutput);
//                formatter.setRepository(repository);
//                if(isIgnore){
//                    formatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL); // 设置过滤空白字符
//                }else{
//                    formatter.setDiffComparator(RawTextComparator.DEFAULT);
//                }
//                formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS));
//                formatter.setDetectRenames(true);
//                formatter.getRenameDetector().setRenameScore(50);
//                formatter.format(diffs);
//                return new Object[]{formatter,diffs};
//            }
//        }
//    }

    public RevCommit getRevCommit(String CommitString) throws IOException {
        RevWalk walk=new RevWalk(this.repository);
        ObjectId commitID = this.repository.resolve(CommitString);
        ObjectReader objectReader = repository.newObjectReader();

        RevWalk revWalk = new RevWalk(objectReader);
        return revWalk.parseCommit(commitID);

    }


    /**
     * 根据名称和文件的存储位置获取类的文件内容
     *
     * @param gitAdapter Git适配器
     * @param Name       分支名 或者CommitName
     * @param JavaPath   Java的路径名称
     * @param isCommit   是否是提交
     * @return 返回该Commit或者分支的下的JavaPath的类文件信息
     */
    static String getClassContent(GitAdapter gitAdapter, String Name, String JavaPath, boolean isCommit) {
        String classInfo = null;
        if (!isCommit) {
            try {
                classInfo = gitAdapter.getBranchSpecificFileContent(Name, JavaPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                classInfo = gitAdapter.getCommitSpecificFileContent(Name, JavaPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return classInfo;
    }


    /**
     * Returns the list of files changed in a specified commit. If the repository does not exist or is empty, an empty list is returned.
     *
     * @param BeforeCommit earliest commit
     * @param endCommit   most recent commit. if null, HEAD is assumed.
     * @return list of files changed in a commit range
     */
    public List<DiffEntry> getFilesInRange(String BeforeCommit, String endCommit) {
        List<DiffEntry> list = new ArrayList<DiffEntry>();
        try {
            ObjectId startRange = this.repository.resolve(BeforeCommit);
            ObjectId endRange = repository.resolve(endCommit);
            try (var rw = new RevWalk(repository)) {
                RevCommit start = rw.parseCommit(startRange);
                RevCommit end = rw.parseCommit(endRange);
                list.addAll(getFilesInRange(repository, start, end));
            }
        } catch (Throwable t) {
        }
        return list;
    }

    /**
     * Returns the list of files changed in a specified commit. If the repository does not exist or is empty, an empty list is returned.
     *
     * @param repository
     * @param startCommit earliest commit
     * @param endCommit   most recent commit. if null, HEAD is assumed.
     * @return list of files changed in a commit range
     */
    private  List<DiffEntry> getFilesInRange(Repository repository, RevCommit startCommit, RevCommit endCommit) {
        List<DiffEntry> diffEntries = null;

        try (var df = CodeDiff.getDiffFormatter(false, DisabledOutputStream.INSTANCE,repository)) {
//            df.setRepository(repository);
//            df.setDiffComparator(RawTextComparator.DEFAULT);
//            df.setDetectRenames(true);

           diffEntries=df.scan(prepareTreeParser(startCommit),prepareTreeParser(endCommit));

        } catch (Throwable t) {
        }

        return diffEntries;
    }


    /**
     * Returns the list of files in the repository in the specified commit that match one of the specified extensions. This is a CASE-SENSITIVE
     * search. If the repository does not exist or is empty, an empty list is returned.
     *
     * @param repository
     * @param extensions
     * @param CommitID
     * @return list of files in repository with a matching extension
     */
    public static List<PathModel> getDocuments(Repository repository, List<String> extensions, String CommitID) throws IOException {
        List<PathModel> list = new ArrayList<PathModel>();
        var walk = new RevWalk(repository);
        ObjectId commitID = repository.resolve(CommitID);
        RevCommit commit = walk.parseCommit(commitID);
        try (var tw = new TreeWalk(repository)) {
            tw.addTree(commit.getTree());
            if (extensions != null && extensions.size() > 0) {
                List<TreeFilter> suffixFilters = new ArrayList<TreeFilter>();
                for (String extension : extensions) {
                    if (extension.charAt(0) == '.') {
                        suffixFilters.add(PathSuffixFilter.create(extension));
                    } else {
                        // escape the . since this is a regexp filter
                        suffixFilters.add(PathSuffixFilter.create("." + extension));
                    }
                }
                TreeFilter filter;
                if (suffixFilters.size() == 1) {
                    filter = suffixFilters.get(0);
                } else {
                    filter = OrTreeFilter.create(suffixFilters);
                }
                tw.setFilter(filter);
                tw.setRecursive(true);
            }
            while (tw.next()) {
                list.add(getPathModel(tw, null, commit));
            }
        } catch (IOException e) {

        }
        Collections.sort(list);
        return list;
    }

    /**
     * Returns a path model of the current file in the treewalk.
     *
     * @param tw
     * @param basePath
     * @param commit
     * @return a path model of the current file in the treewalk
     */
    private static PathModel getPathModel(TreeWalk tw, String basePath, RevCommit commit) {
        String name;
        long size = 0;
        if (basePath == null && basePath.trim().equals("")) {
            name = tw.getPathString();
        } else {
            name = tw.getPathString().substring(basePath.length() + 1);
        }
        var objectId = tw.getObjectId(0);
        try {
            if (!tw.isSubtree() && (tw.getFileMode(0) != FileMode.GITLINK)) {
                size = tw.getObjectReader().getObjectSize(objectId, Constants.OBJ_BLOB);
            }
        } catch (Throwable t) {
            System.out.println(t.toString());
        }
        return new PathModel(name, tw.getPathString(), size, tw.getFileMode(0).getBits(), objectId.getName(), commit.getName());
    }


    /**
     * Returns a list of commits since the minimum date starting from the specified object id.
     *
     * @param objectId    if unspecified, HEAD is assumed.
     * @param minimumDate
     * @return list of commits
     */
    public List<RevCommit> getRevLog(String objectId, Date minimumDate) {
        List<RevCommit> list = new ArrayList<RevCommit>();

        try {
            // resolve branch
            ObjectId branchObject = repository.resolve(objectId);

            var rw = new RevWalk(repository);
            rw.markStart(rw.parseCommit(branchObject));
            rw.setRevFilter(CommitTimeRevFilter.after(minimumDate));
            Iterable<RevCommit> revlog = rw;
            for (RevCommit rev : revlog) {
                list.add(rev);
            }
            rw.close();
            rw.dispose();
        } catch (Throwable t) {
        }
        return list;
    }

    /**
     * Returns a list of commits starting from HEAD and working backwards.
     *
     * @param maxCount   if < 0, all commits for the repository are returned.
     * @return list of commits
     */
    public List<RevCommit> getRevLog(int maxCount) {
        return getRevLog(null, 0, maxCount);
    }

    /**
     * Returns a list of commits starting from the specified objectId using an offset and maxCount for paging. This is similar to LIMIT n OFFSET p in
     * SQL. If the repository does not exist or is empty, an empty list is returned.
     *
     * @param objectId   if unspecified, HEAD is assumed.
     * @param offset
     * @param maxCount   if < 0, all commits are returned.
     * @return a paged list of commits
     */
    public List<RevCommit> getRevLog(String objectId, int offset, int maxCount) {
        return getRevLog(objectId, null, offset, maxCount);
    }

    /**
     * Returns a list of commits for the repository or a path within the repository. Caller may specify ending revision with objectId. Caller may
     * specify offset and maxCount to achieve pagination of results. If the repository does not exist or is empty, an empty list is returned.
     * @param objectId   if unspecified, HEAD is assumed.
     * @param path       if unspecified, commits for repository are returned. If specified, commits for the path are returned.
     * @param offset
     * @param maxCount   if < 0, all commits are returned.
     * @return a paged list of commits
     */
    public List<RevCommit> getRevLog(String objectId, String path, int offset, int maxCount) {
        List<RevCommit> list = new ArrayList<RevCommit>();
        if (maxCount == 0) {
            return list;
        }

        try {
            // resolve branch
            ObjectId startRange = null;
            ObjectId endRange;
            if (objectId.contains("..")) {
                // range expression
                String[] parts = objectId.split("\\.\\.");
                startRange = repository.resolve(parts[0]);
                endRange = repository.resolve(parts[1]);
            } else {
                // objectid
                endRange = repository.resolve(objectId);
            }

            if (endRange == null) {
                return list;
            }

            var rw = new RevWalk(repository);
            rw.markStart(rw.parseCommit(endRange));
            if (startRange != null) {
                rw.markUninteresting(rw.parseCommit(startRange));
            }
            if (path != null && path.trim().equals("")) {
                var filter = AndTreeFilter.create(PathFilterGroup.createFromStrings(Collections.singleton(path)), TreeFilter.ANY_DIFF);
                rw.setTreeFilter(filter);
            }
            Iterable<RevCommit> revlog = rw;
            if (offset > 0) {
                var count = 0;
                for (RevCommit rev : revlog) {
                    count++;
                    if (count > offset) {
                        list.add(rev);
                        if (maxCount > 0 && list.size() == maxCount) {
                            break;
                        }
                    }
                }
            } else {
                for (RevCommit rev : revlog) {
                    list.add(rev);
                    if (maxCount > 0 && list.size() == maxCount) {
                        break;
                    }
                }
            }
            rw.close();
            rw.dispose();
        } catch (Throwable t) {
        }
        return list;
    }

    /**
     * Returns a list of commits for the repository within the range specified by startRangeId and endRangeId. If the repository does not exist or is
     * empty, an empty list is returned.
     *
     * @param startRangeId the first commit (not included in results)
     * @param endRangeId   the end commit (included in results)
     * @return a list of commits
     */
    public List<RevCommit> getRevLog(String startRangeId, String endRangeId) {
        List<RevCommit> list = new ArrayList<RevCommit>();

        try {
            ObjectId endRange = repository.resolve(endRangeId);
            ObjectId startRange = repository.resolve(startRangeId);

            var rw = new RevWalk(repository);
            rw.markStart(rw.parseCommit(endRange));
            if (startRange.equals(ObjectId.zeroId())) {
                // maybe this is a tag or an orphan branch
                list.add(rw.parseCommit(endRange));
                rw.close();
                rw.dispose();
                return list;
            } else {
                rw.markUninteresting(rw.parseCommit(startRange));
            }

            Iterable<RevCommit> revlog = rw;
            for (RevCommit rev : revlog) {
                list.add(rev);
            }
            rw.close();
            rw.dispose();
        } catch (Throwable t) {
            System.out.println(t.toString());
        }
        return list;
    }


    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public String getProjectName() {
        return localPath.substring(this.localPath.lastIndexOf("/") + 1);
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }


    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    /**
     * whether the file exists under the commit tree
     * @param commitMessage
     * @param Path
     * @return Null: There is no such file in the Commit；
     *         DiffEntry: The file exists and returns the diff
     */
    public DiffEntry getDiffOfFileInCommit(CommitMessage commitMessage, String  Path) throws IOException {
        var diffEntries=this.getFilesInRange(commitMessage.getLastCommitId(),commitMessage.getCommitId());
        for(DiffEntry diffEntry:diffEntries){
            String path=diffEntry.getChangeType()== DiffEntry.ChangeType.DELETE? diffEntry.getOldPath():diffEntry.getNewPath();
            if(path.equals(Path)&&(CodeDiff.getDiffFormatter(true,null,this.repository)
            .toFileHeader(diffEntry).toEditList().size()!=0)){
                return diffEntry ;
            }
        }
        return null;
    }




}