package Action;

import Obj.ClassInfo;
import Obj.MethodInfo;
import com.google.common.base.Charsets;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CodeDiff {

    /**
     * 分支和分支之间的比较
     *
     * @param newBranchName 当前分支取最新的commit版本
     * @param oldBranchName 比较分支取最新的commit版本
     * @return 返回分支和分支之间的不同的内容列表
     */
    public static List<ClassInfo> diffBranchToBranch(GitAdapter gitAdapter, String newBranchName, String oldBranchName) {
        return diffBranchToBranch(gitAdapter, newBranchName, oldBranchName, null);
    }

    /**
     * 当前分支与某个分支的某个版本进行比较
     *
     * @param gitAdapter    Git适配器
     * @param newBranchName 当前分支当前分支取最新的commit版本
     * @param oldBranchName 比较分支
     * @param commitId      比较分支的commit版本Id
     * @return list: 分支与某个分支的某个版本的不同内容
     */
    public static List<ClassInfo> diffBranchToBranch(GitAdapter gitAdapter, String newBranchName, String oldBranchName, String commitId) {
        return diffMethods(gitAdapter, newBranchName, oldBranchName, commitId);
    }


    /**
     * diffBranchToBranch的内部函数
     *
     * @param gitAdapter    获取的Git的初始化器
     * @param newBranchName Git的新分支的名字
     * @param oldBranchName Git旧分支的名字
     * @param commitId      提交的Commit
     * @return list: 分支与某个分支的某个版本的不同内容
     */
    private static List<ClassInfo> diffMethods(GitAdapter gitAdapter, String newBranchName, String oldBranchName, String commitId) {
        try {
            //  获取Git
            Git git = gitAdapter.getGit();

            //  获取两个分支的代码信息
            Ref newBranchRef = gitAdapter.checkOutAndPull(newBranchName);
            Ref oldBranchRef = gitAdapter.checkOutAndPull(oldBranchName);

            //  获取当前分支信息
            AbstractTreeIterator newTreeParser = gitAdapter.prepareTreeParser(newBranchRef.getObjectId().getName());

            //  获取比较分支的版本信息 如果commit为null，则默认取比较分支的最新版本信息
            if (null == commitId && oldBranchRef != null) {
                commitId = oldBranchRef.getObjectId().getName();
            }
            AbstractTreeIterator oldTreeParser = gitAdapter.prepareTreeParser(commitId);

            //  对比差异
            List<DiffEntry> diffs = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).setShowNameAndStatusOnly(true).call();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter(out);
            //  设置比较器为忽略空白字符对比（Ignores all whitespace）
            df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            df.setRepository(git.getRepository());
            return batchPrepareDiffMethod(gitAdapter, newBranchName, oldBranchName, df, diffs, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * 比较两个提交之间的具体信息
     *
     * @param gitAdapter Git适配器
     * @param newCommit  新的提交SHA1
     * @param oldCommit  父提交SHA1
     * @param isIgnore   是否忽略空白字符
     * @return list: 返回前后版本的不同内容的列表
     */

    public static List<ClassInfo> diff2Commit_concrete(GitAdapter gitAdapter, String newCommit, String oldCommit, boolean isIgnore) {
        try {
            //  获取Git
            Git git = gitAdapter.getGit();


            //获取对应的树
            AbstractTreeIterator newTreeParser = gitAdapter.prepareTreeParser(newCommit);

            AbstractTreeIterator odlTreeParser = gitAdapter.prepareTreeParser(oldCommit);


            //  对比差异
            List<DiffEntry> diffs = git.diff().setOldTree(odlTreeParser).setNewTree(newTreeParser).setShowNameAndStatusOnly(true).call();


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            DiffFormatter df = getDiffFormatter(isIgnore, out, git.getRepository());

            return batchPrepareDiffMethod(gitAdapter, newCommit, oldCommit, df, diffs, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    /**
     * 获取空白字符的文件列表
     *
     * @param gitAdapter
     * @param newCommit
     * @param oldCommit
     * @return
     */
    public static List<DiffEntry>[] GetChanges_ByBlank(GitAdapter gitAdapter, String newCommit, String oldCommit) {
        try {

            //  获取Git
            Git git = gitAdapter.getGit();

            //  Total对比差异
            List<DiffEntry> diffs = gitAdapter.getFilesInRange(oldCommit, newCommit);

   //         System.out.println(newCommit+" "+diffs.size());

        //    ByteArrayOutputStream out = new ByteArrayOutputStream();

        //    DiffFormatter df = getDiffFormatter(true, out, git.getRepository());
//            System.out.println(diffs.get(0).getChangeType()+" "+diffs.get(0).);
//
//            System.out.println("first 进入");

            var BlackIndex = diffs.stream().mapToInt(x -> {
                try {

                    FileHeader fileHeader = getDiffFormatter(true, DisabledOutputStream.INSTANCE, git.getRepository()).toFileHeader(x);

                    EditList editList = fileHeader.toEditList();   //已经进行过滤了

                    if (editList.size() == 0) {
                        return 1;
                    } else {
                        return 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }).toArray();


            var BlackList = new ArrayList<DiffEntry>();
            var NonBlackList = new ArrayList<DiffEntry>();

            for (int i = 0; i < BlackIndex.length; i++) {
                if (BlackIndex[i] == 1) {    //为空白的DiffEntry
                    BlackList.add(diffs.get(i));
                } else {
                    NonBlackList.add(diffs.get(i));
                }
            }

            //      System.out.println(newCommit+"::"+diffs.size()+" "+NonBlackList.size()+" "+BlackList.size());

            return new List[]{NonBlackList, BlackList};

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 多线程执行对比: diff2Commit_concrete diffMethods 的实际调用函数；
     *
     * @param isCommit 是否是提交 True:是提交
     * @param newName  新分支或者新Commit
     * @param oldName  旧分支或者旧Commit
     * @return 返回执行对比结果
     */
    public static List<ClassInfo> batchPrepareDiffMethod(final GitAdapter gitAdapter, final String newName, final String oldName, final DiffFormatter df, List<DiffEntry> diffs, Boolean isCommit) {
        int threadSize = 100;
        int dataSize = diffs.size();   //两课树的diff数量
        int threadNum = dataSize / threadSize + 1;
        boolean special = dataSize % threadSize == 0;
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);

        List<Callable<List<ClassInfo>>> tasks = new ArrayList<>();
        Callable<List<ClassInfo>> task;  //执行任务并返回值；
        List<DiffEntry> cutList;
        //  分解每条线程的数据
        for (int i = 0; i < threadNum; i++) {
            /*
            对diff的数据进行肢解
             */
            if (i == threadNum - 1) {
                if (special) {
                    break;
                }
                cutList = diffs.subList(threadSize * i, dataSize);
            } else {
                cutList = diffs.subList(threadSize * i, threadSize * (i + 1));
            }

            final List<DiffEntry> diffEntryList = cutList;
            task = () -> {
                List<ClassInfo> allList = new ArrayList<>();
                for (DiffEntry diffEntry : diffEntryList) {
                    ClassInfo classInfo;
                    if (isCommit) {
                        classInfo = prepareDiffMethod(gitAdapter, newName, oldName, df, diffEntry, true);
                    } else {
                        classInfo = prepareDiffMethod(gitAdapter, newName, oldName, df, diffEntry, false);
                    }
//                        if(classInfo==null){
//                            System.out.println(diffEntry.getChangeType()+" "+diffEntry.getNewPath());
//                        }
                    if (classInfo != null) {
                        classInfo.setCommitID(newName);
                        allList.add(classInfo);
                    }
                }
                return allList;
            };
            // 这里提交的任务容器列表和返回的Future列表存在顺序对应的关系
            tasks.add(task);
        }
        List<ClassInfo> allClassInfoList = new ArrayList<>();
        try {
            List<Future<List<ClassInfo>>> results = executorService.invokeAll(tasks);
            //结果汇总
            for (Future<List<ClassInfo>> future : results) {
                allClassInfoList.addAll(future.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭线程池
            executorService.shutdown();
        }
        return allClassInfoList;
    }


    /**
     * 单个差异文件对比
     *
     * @param gitAdapter Git适配器
     * @param newCommit    新的分支名称 或者新的提交名称
     * @param oldCommit    旧的分支名称 或者旧的提交名称
     * @param df         控制Diff的输出格式；满足条件的才能输出
     * @param diffEntry  Diff条目
     * @param isCommit   是否是提交信息
     * @return 返回DiffEntry的具体类对比信息；
     */
    public static synchronized  ClassInfo prepareDiffMethod(GitAdapter gitAdapter, String newCommit, String oldCommit, DiffFormatter df, DiffEntry diffEntry, boolean isCommit) {
        List<MethodInfo> methodInfoList = new ArrayList<>();
        try {
            String newJavaPath = diffEntry.getNewPath();
//            //  排除测试类
//            if (newJavaPath.contains("/src/test/java/")) {
//                return null;
//            }
            //  非java文件 和 删除类型不记录
//            if (!newJavaPath.endsWith(".java") ) {
//                return null;
//            }
            /*  新增类型   */

            if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD) {  //如果是ADD类型，则直接把新添加的类的信息导出
                String newClassContent = GitAdapter.getClassContent(gitAdapter, newCommit, newJavaPath, isCommit);
                ASTGenerator newAstGenerator = new ASTGenerator(newClassContent);
                //获得改变的行数
                List<int[]> addLines = new ArrayList<>();
                List<int[]> delLines = new ArrayList<>();
                getChangeLines(df, diffEntry, addLines, delLines);

                return newAstGenerator.getClassInfo(gitAdapter.getProjectName(),translate(addLines), translate(delLines), DiffEntry.ChangeType.ADD.toString(), diffEntry.getNewPath(), newClassContent, diffEntry.getId(DiffEntry.Side.NEW));
            } else if (diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE) {//如果是Delete类型，直接把旧版本的类的信息导出
                String oldJavaPath = diffEntry.getOldPath();
                String oldClassContent = GitAdapter.getClassContent(gitAdapter, oldCommit, oldJavaPath, isCommit);
                ASTGenerator oldAstGenerator = new ASTGenerator(oldClassContent);
                //获得改变的行数
                List<int[]> addLines = new ArrayList<>();
                List<int[]> delLines = new ArrayList<>();
                getChangeLines(df, diffEntry, addLines, delLines);

                return oldAstGenerator.getClassInfo(gitAdapter.getProjectName(),translate(addLines), translate(delLines), DiffEntry.ChangeType.DELETE.toString(), diffEntry.getOldPath(), oldClassContent, diffEntry.getId(DiffEntry.Side.OLD));
            }
//            System.out.println("Enter: "+(gitAdapter==null)+" "+newCommit+" "+newJavaPath+" "+isCommit);
            String newClassContent = GitAdapter.getClassContent(gitAdapter, newCommit, newJavaPath, isCommit);
            ASTGenerator newAstGenerator = new ASTGenerator(newClassContent);

            String oldJavaPath = diffEntry.getOldPath();

            String oldClassContent = GitAdapter.getClassContent(gitAdapter, oldCommit, oldJavaPath, isCommit);
            ASTGenerator oldAstGenerator = new ASTGenerator(oldClassContent);

            /*  修改类型  */
            //  获取文件差异位置，从而统计差异的行数，如增加行数，减少行数
            List<int[]> addLines = new ArrayList<>();
            List<int[]> delLines = new ArrayList<>();

            if (getChangeLines(df, diffEntry, addLines, delLines)) {
                return null;
            }

            MethodDeclaration[] newMethods = newAstGenerator.getMethods();
            MethodDeclaration[] oldMethods = oldAstGenerator.getMethods();
            Map<String, MethodDeclaration> methodsMap = new HashMap<>();
            //方法Map：方法的名字和参数：方法；
            for (MethodDeclaration oldMethod : oldMethods) {
                methodsMap.put(oldMethod.getName().toString() + oldMethod.parameters().toString(), oldMethod);
            }
            for (final MethodDeclaration method : newMethods) {
                // 如果方法名是新增的,则直接将方法加入List
                if (!ASTGenerator.isMethodExist(method, methodsMap)) {
                    MethodInfo methodInfo = newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                    continue;
                }
                // 如果两个版本都有这个方法,则根据MD5判断方法是否一致
                if (!ASTGenerator.isMethodTheSame(method, methodsMap.get(method.getName().toString() + method.parameters().toString()))) {
                    MethodInfo methodInfo = newAstGenerator.getMethodInfo(method);
                    methodInfoList.add(methodInfo);
                }
            }
            //modify 和 copy 类型
            return newAstGenerator.getClassInfo(gitAdapter.getProjectName(),methodInfoList, translate(addLines), translate(delLines), diffEntry.getChangeType().toString(), diffEntry.getNewPath(), newClassContent, diffEntry.getId(DiffEntry.Side.NEW));
        } catch (Exception e) {
            System.out.println(newCommit);
            e.printStackTrace();
        }
        return null;
    }

    public static boolean getChangeLines(DiffFormatter df, DiffEntry diffEntry, List<int[]> addLines, List<int[]> delLines) throws IOException {
        FileHeader fileHeader = df.toFileHeader(diffEntry);
        EditList editList = fileHeader.toEditList();   //已经进行过滤了
        if (editList.size() == 0) {
            return true;
        }
            /*
            获取的是删除和添加的代码行
             */
        for (Edit edit : editList) {
            if (edit.getLengthA() > 0) {
                delLines.add(new int[]{edit.getBeginA(), edit.getEndA()});
            }
            if (edit.getLengthB() > 0) {
                addLines.add(new int[]{edit.getBeginB(), edit.getEndB()});
            }
        }
        return false;
    }


    /**
     * 将修改文件的行数信息 int[] 转化为list类型
     * diff返回的int[] 表示修改的区间，修改区间是左开右闭（ 】
     *
     * @param listInt ADD or Delete 的修改的区间，修改区间是左开右闭（ 】
     * @return Add or Delete 的行标号
     */
    public static List<Integer> translate(List<int[]> listInt) {
        List<Integer> list = new ArrayList<>();
        for (int[] ints : listInt) {
            for (int i = ints[0]; i < ints[1]; i++) {
                list.add(i + 1);
            }
        }
        return list;
    }

    /**
     * 返回两个版本的对比结果
     *
     * @param gitAdapter Git示例
     * @param newCommit  较新的Commit ID
     * @param oldCommit  较旧的Commit ID
     * @return Object[]: Object[0] 格式器 ; Object[1] 获得前后版本的diffs
     */
    public static Object[] diffCommitToCommit(GitAdapter gitAdapter, String newCommit, String oldCommit, boolean isIgnore) throws IOException, GitAPIException {

        Git git = gitAdapter.getGit();

        AbstractTreeIterator oldTreeParser = gitAdapter.prepareTreeParser(oldCommit);

        AbstractTreeIterator newTreeParser = gitAdapter.prepareTreeParser(newCommit);

        //  对比差异
        List<DiffEntry> diffs = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).setShowNameAndStatusOnly(true).call();

        try (OutputStream tmpOutput = new ByteArrayOutputStream(2048)) {
            DiffFormatter formatter = getDiffFormatter(isIgnore, tmpOutput, gitAdapter.getRepository());
            formatter.format(diffs);

            return new Object[]{formatter, diffs};
        }
    }

    /**
     * 设置Diffentry的输出格式
     *
     * @param isIgnore   是否忽略空白字符
     * @param tmpOutput  流对象
     * @param repository 仓库对象
     */
    public static DiffFormatter getDiffFormatter(boolean isIgnore, OutputStream tmpOutput, Repository repository) {
        DiffFormatter formatter = new DiffFormatter(tmpOutput);
        formatter.setRepository(repository);
        if (isIgnore) {
            formatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL); // 设置过滤空白字符
        } else {
            formatter.setDiffComparator(RawTextComparator.DEFAULT);
        }
        formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS));
        formatter.setDetectRenames(true); //出现的重命名
        formatter.getRenameDetector().setRenameScore(50); // 相似性差值
        return formatter;
    }

    /**
     * 返回两个File的Diff
     *
     * @param file1 新的File文件
     * @param file2 旧的File文件
     * @return 返回Diff的String
     */
    public static String getDiff(File file1, File file2) {

        OutputStream out = new ByteArrayOutputStream();
        EditList diffList = null;
        try {
            RawText rt1 = new RawText(file1);
            RawText rt2 = new RawText(file2);
            diffList = new EditList();
            diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, rt1, rt2));
            new DiffFormatter(out).format(diffList, rt1, rt2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  return diffList;
        return out.toString();
    }


    public static String getDiff(String beforeConent, String afterContent) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RawText beforeRawText = new RawText(beforeConent.getBytes(Charsets.UTF_8));
        RawText afeterRawText = new RawText(afterContent.getBytes(Charsets.UTF_8));
        EditList diffList = new EditList();
        diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, beforeRawText, afeterRawText));
        new DiffFormatter(out).format(diffList, beforeRawText, afeterRawText);
        return out.toString(Charsets.UTF_8.name());
    }

}