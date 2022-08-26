import Action.CodeDiff;
import Action.GitAdapter;
import Action.GumTreeAdatpter;
import Bean.FeatureDo;
import Date.DateAction;
import Extractor.CodeFormatter;
import Extractor.RefDiffExtractor;
import Extractor.RefactoringMine;
import IntervalTreeInstance.IntegerInterval;
import IntervalTreeInstance.Interval;
import IntervalTreeInstance.IntervalTree;
import Obj.CommitMessage;
import PareCode.PareInstance;
import SimilarityTool.LCS_local;
import Tool.FileTool;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

interface RuleFilter {
    /**
     * 定义的过滤规则实现，通过该接口定制不同的过滤规则
     *
     * @param editScript: GumTree diff Entries
     */
    void filter(EditScript editScript);
}

class FinalFilter implements RuleFilter {
    @Override
    public void filter(EditScript editScript) {

        Iterator<Action> iterator = editScript.iterator();
        while (iterator.hasNext()) {
            var action = iterator.next();

            var nodeType = action.getNode().getType().toString();
            if ("Modifier".equalsIgnoreCase(nodeType)) {
                iterator.remove();
//                if ("Parameter".equalsIgnoreCase(action.getNode().getParent().getType().toString())
//                        || "VariableDeclarationExpr".equalsIgnoreCase(action.getNode().getParent().getType().toString())) {
//                    if ("final".equalsIgnoreCase(action.getNode().getLabel())) {
//                        iterator.remove();
//                    }
//                }
            }
        }
    }
}


class AnnotationFilter implements RuleFilter {

    @Override
    public void filter(EditScript editScript) {
        Iterator<Action> iterator = editScript.iterator();
        while (iterator.hasNext()) {
            var action = iterator.next();
            var nodeType = action.getNode().getType().toString();
            if ("MarkerAnnotationExpr".equalsIgnoreCase(nodeType) || "SingleMemberAnnotationExpr".equalsIgnoreCase(nodeType)
                    || "MarkerAnnotationExpr".equalsIgnoreCase(nodeType)) {
                iterator.remove();
            }
        }
    }
}

class tokenRelatedFilter {

    /**
     * @param proeditScript
     * @param testeditScript
     * @param v
     * @return True: 相似； False: 不相似
     */
    public static boolean isSimilar(EditScript proeditScript, EditScript testeditScript, double v) {
        //设置一个阈值
        var simThreshold = v;

        if (getScore(proeditScript, testeditScript) > simThreshold) {
            return true;
        } else {
            return false;
        }
    }

    public static double getScore(EditScript proeditScript, EditScript testeditScript) {
        var set1 = new HashSet<String>();
        var set2 = new HashSet<String>();
        proeditScript.forEach(x -> FileTool.getLabel(x.getNode(), set1));
        testeditScript.forEach(x -> FileTool.getLabel(x.getNode(), set2));
        return FileTool.GetTokenRelated(set1, set2);
    }

    public static double getScore(HashSet<String> proset, HashSet<String> testset) throws IOException {
        return FileTool.GetTokenRelated(proset, testset);
    }


    public static Pair<HashSet<String>, HashSet<String>> getTokens(GitAdapter gitAdapter, DiffEntry diffentry, String newContent, String oldContent, CommitMessage commitMessage) throws IOException {


        List<int[]> addLines = new ArrayList<>();

        List<int[]> delLines = new ArrayList<>();

        CodeDiff.getChangeLines(CodeDiff.getDiffFormatter(true, null, gitAdapter.getRepository()), diffentry, addLines, delLines);

        var addLinesLists = CodeDiff.translate(addLines);   //Tanslate changed lines

        var delLinesLists = CodeDiff.translate(delLines);  //Tanslate changed lines

        addLinesLists = FileterCommentChanges(addLinesLists, newContent); //得到不是Commit

        delLinesLists = FileterCommentChanges(delLinesLists, oldContent);

        addLinesLists = FilterImportChanges(addLinesLists, newContent); //得到不是impor

        delLinesLists = FilterImportChanges(delLinesLists, oldContent);


        HashSet<String> result_add = new HashSet<>();

        HashSet<String> result_delete = new HashSet<>();

        addLinesLists.forEach(index -> {
            try {
                result_add.addAll(tokenize_java_code_origin(FileTool.getContent(newContent, index)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        delLinesLists.forEach(index -> {
            try {
                result_delete.addAll(tokenize_java_code_origin(FileTool.getContent(oldContent, index)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return Pair.of(result_add, result_delete);
    }


    private static List<Integer> FileterCommentChanges(List<Integer> ChangedLinesLists, String Content) {
        var intervalTree = PareInstance.getIntervalThroughTypes(Content); //Comment的行
        return ChangedLinesLists.stream().filter(x -> {
            return intervalTree.query(x).size() == 0;   //当前的更改行在intervalTree中查询，如果size==0代表没有查询到，即不是Comment修改，保留
        }).collect(Collectors.toList());
    }

    private static List<Integer> FilterImportChanges(List<Integer> ChangedLinesLists, String Content) {
        var intervalTree = PareInstance.getImportsInterval(Content); //Imports的行
        return ChangedLinesLists.stream().filter(x -> {
            return intervalTree.query(x).size() == 0;   //当前的更改行在intervalTree中查询，如果size==0代表没有查询到，即不是impors修改
        }).collect(Collectors.toList());

    }

    /**
     * Token 化
     *
     * @param content
     * @return
     */

    public static HashSet<String> tokenize_java_code_origin(String content) {
        var transcontent = content.strip();
        var tokens = transcontent.split("\\s+");
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < tokens.length; i++) {
            String result[] = tokens[i].split("([-!\"#$%&\\'()*+,./:;<=>?@\\[\\\\\\]^`{|}~])");
            for (int j = 0; j < result.length; j++) {
                if (result[j].strip().equals("")) {
                    continue;
                }
                set.add(result[j].strip());
            }
        }
        return set;
    }


    /**
     * @param editScript1
     * @param editScript2
     * @param v
     * @return True 不相似； False: 相似
     */
    public static boolean NonSimilar(EditScript editScript1, EditScript editScript2, double v) {
        return !isSimilar(editScript1, editScript2, v);
    }
}

class FilterAction {


    /**
     * @param gitAdapter     : The Git action
     * @param commitMessages : Store th commit history. Node that last commit is in size=0
     * @param featureDo      Code change Pair
     * @param flag           True:保留 False:删除
     * @return: （TRUE,TRUE) => 保留且需要更改标签； (TRUE, FALSE) => 保留且不需要更改标签  (FALSE,NULL) => 噪音
     */
    public static Pair<Boolean, Boolean> isKeepFeatureDo(GitAdapter gitAdapter, List<CommitMessage> commitMessages, final FeatureDo featureDo, boolean flag) throws IOException {
        // 获取Pro的CommitMessage
        //初筛

        var proIndex = getIndexCommit(featureDo.getPro_Commit(), commitMessages);
        var testIndex = getIndexCommit(featureDo.getTest_Commit(), commitMessages);
        var proCommit = commitMessages.get(proIndex);
        var testCommit = commitMessages.get(testIndex);
        var proDiff = gitAdapter.getDiffOfFileInCommit(proCommit, featureDo.getProd_path());
        var testDiff = gitAdapter.getDiffOfFileInCommit(testCommit, featureDo.getTest_path());
        //还是以SITAR的标签作为了一个判断
        if ("negative".equalsIgnoreCase(featureDo.getLabel())) {
            //Rule1: no other production code changes and change type is not modification
            if ((proDiff.getChangeType() != DiffEntry.ChangeType.MODIFY)
                    || (testDiff.getChangeType() != DiffEntry.ChangeType.MODIFY)) {
                if (!hasOtherPro(gitAdapter, featureDo.getProd_path(), proIndex, testIndex, commitMessages) && !hasOtherTest(gitAdapter, featureDo.getTest_path(), proIndex, testIndex, commitMessages)) {
                    return Pair.of(flag, Boolean.TRUE);
                }
            }
            return Pair.of(Boolean.TRUE, Boolean.FALSE);
        } else {
            EditScript proEditScrips = null;
            EditScript testEditScrips = null;
            String proOldContent, proNewContent, testOldContent, testNewContent;
            if (proIndex < testIndex) {
                return Pair.of(flag, Boolean.TRUE);
            }

            if (!featureDo.getPro_Commit().equalsIgnoreCase(featureDo.getTest_Commit())) {
                // Rule2: has other production code.
                if (hasOtherPro(gitAdapter, featureDo.getProd_path(), proIndex, testIndex, commitMessages)) {
                    return Pair.of(flag, Boolean.TRUE);
                }

                // Rule1: the time intervals is more than 12 hours, 考虑到是SITAR.
                if (Math.abs(DateAction.get_diff(featureDo.getTest_time(), featureDo.getProd_time())) > 43200) {
                    if ((proDiff.getChangeType() != DiffEntry.ChangeType.MODIFY)
                            || (testDiff.getChangeType() != DiffEntry.ChangeType.MODIFY)) {
                        if (!hasOtherPro(gitAdapter, featureDo.getProd_path(), proIndex, testIndex, commitMessages) && !hasOtherTest(gitAdapter, featureDo.getTest_path(), proIndex, testIndex, commitMessages)) {
                            return Pair.of(Boolean.TRUE, Boolean.FALSE);
                        }
                    }
                    return Pair.of(flag, Boolean.TRUE);  //保留且需要更改标签
                }

                if (proDiff.getChangeType() != DiffEntry.ChangeType.MODIFY || testDiff.getChangeType() != DiffEntry.ChangeType.MODIFY) {
                    return Pair.of(Boolean.TRUE, Boolean.FALSE);
                }

            } else {
                if (proDiff.getChangeType() != DiffEntry.ChangeType.MODIFY || testDiff.getChangeType() != DiffEntry.ChangeType.MODIFY) {
                    return Pair.of(Boolean.TRUE, Boolean.FALSE);
                }
            }
            proOldContent = gitAdapter.getCommitSpecificFileContent(proCommit.getLastCommitId(), proDiff.getOldPath());
            proNewContent = gitAdapter.getCommitSpecificFileContent(proCommit.getCommitId(), proDiff.getNewPath());
            testOldContent = gitAdapter.getCommitSpecificFileContent(testCommit.getLastCommitId(), testDiff.getOldPath());
            testNewContent = gitAdapter.getCommitSpecificFileContent(testCommit.getCommitId(), testDiff.getNewPath());

            if (isOnlyImpors(gitAdapter, proDiff, proNewContent, proOldContent, proCommit)
                    || isOnlyImpors(gitAdapter, testDiff, testNewContent, testOldContent, testCommit)) {
                var pro = getImportsTokens(gitAdapter, proDiff, proNewContent, proOldContent, proCommit);
                var test = getImportsTokens(gitAdapter, testDiff, testNewContent, testOldContent, testCommit);
                if (test.size() == 0 || pro.size() == 0) {
                    return Pair.of(flag, Boolean.TRUE);
                }
                for (int i = 0; i < test.size(); i++) {
                    if (!pro.contains(test.get(i))) {
                        return Pair.of(flag, Boolean.TRUE);
                    }
                }
            }

            var protokens = new tokenRelatedFilter().getTokens(gitAdapter, proDiff, proNewContent, proOldContent, proCommit);

            HashSet<String> proset_add = protokens.getLeft();
            HashSet<String> proset_del = protokens.getRight();

            var testtokens = new tokenRelatedFilter().getTokens(gitAdapter, testDiff, testNewContent, testOldContent, testCommit);
            HashSet<String> testset_add = testtokens.getLeft();
            HashSet<String> testset_del = testtokens.getRight();

            if (new tokenRelatedFilter().getScore(proset_add, testset_add) == 0.0 &&
                    new tokenRelatedFilter().getScore(proset_del, testset_del) == 0.0) {
                return Pair.of(flag, Boolean.TRUE);
            }

            // Optional: Rule 7: As for the imports information
            if (IsOnlyRefactor(gitAdapter, testDiff, testNewContent, testOldContent, testCommit, featureDo.getTest_path())) {
                return Pair.of(flag, Boolean.TRUE);  //保留且需要更改标签
            }

            proEditScrips = GumTreeAdatpter.getInstance().GetActions(proOldContent, proNewContent);
            testEditScrips = GumTreeAdatpter.getInstance().GetActions(testOldContent, testNewContent);

            if (proEditScrips == null || testEditScrips == null || proEditScrips.size() == 0 || testEditScrips.size() == 0) {
                return Pair.of(Boolean.TRUE, Boolean.FALSE);
            } else {
                // Rule 5： Filter Final modification
                if (!isRelated(proEditScrips, new FinalFilter()) || !isRelated(testEditScrips, new FinalFilter())) {
                    return Pair.of(flag, Boolean.TRUE);  //保留且需要更改标签
                }
                //Rule 6： Filter Annotation modification
                if (!isRelated(proEditScrips, new AnnotationFilter()) || !isRelated(testEditScrips, new AnnotationFilter())) {
                    return Pair.of(flag, Boolean.TRUE);  //保留且需要更改标签
                }
            }
            return Pair.of(Boolean.TRUE, Boolean.FALSE);
        }
    }

    public static boolean isOnlyImpors(GitAdapter gitAdapter, DiffEntry diffentry, String newContent, String oldContent, CommitMessage commitMessage) throws IOException {
        List<int[]> addLines = new ArrayList<>();
        List<int[]> delLines = new ArrayList<>();

        CodeDiff.getChangeLines(CodeDiff.getDiffFormatter(true, null, gitAdapter.getRepository()), diffentry, addLines, delLines);

        var addLinesLists = CodeDiff.translate(addLines);   //Tanslate changed lines

        var delLinesLists = CodeDiff.translate(delLines);  //Tanslate changed lines

        //TODO: 过滤Comment所在的行数

        addLinesLists = isImports(addLinesLists, newContent);

        delLinesLists = isImports(delLinesLists, oldContent);

        if (addLinesLists.isEmpty() && delLinesLists.isEmpty()) {  //其他修改为空
            return true;
        } else {
            return false;
        }

    }

    private static List<Integer> isImports(List<Integer> ChangedLinesLists, String Content) {
        var intervalTree = PareInstance.getImportsInterval(Content); //Comment的行
        return ChangedLinesLists.stream().filter(x -> {
            return intervalTree.query(x).size() == 0;   //当前的更改行在intervalTree中查询，如果size==0代表没有查询到，即不是impors修改
        }).collect(Collectors.toList());
    }

    private static List<String> extractChangedImports(EditScript EditScrips) {
        Iterator<com.github.gumtreediff.actions.model.Action> iterator = EditScrips.iterator();
        var list = new ArrayList<String>();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            if ("ImportDeclaration".equalsIgnoreCase(action.getNode().getType().toString())) {
                list.add(action.getNode().getChild(0).getLabel());
            }
        }
        return list;
    }

    static final String[] internals = new String[]{
            null, "'abstract'", "'assert'", "'boolean'", "'break'", "'byte'", "'case'",
            "'catch'", "'char'", "'class'", "'const'", "'continue'", "'default'",
            "'do'", "'double'", "'else'", "'enum'", "'extends'", "'final'", "'finally'",
            "'float'", "'for'", "'if'", "'goto'", "'implements'", "'import'", "'instanceof'",
            "'int'", "'interface'", "'long'", "'native'", "'new'", "'package'", "'private'",
            "'protected'", "'public'", "'return'", "'short'", "'static'", "'strictfp'",
            "'super'", "'switch'", "'synchronized'", "'this'", "'throw'", "'throws'",
            "'transient'", "'try'", "'void'", "'volatile'", "'while'", null, null,
            null, null, null, null, null, null, null, "'null'", "'('", "')'", "'{'",
            "'}'", "'['", "']'", "';'", "','", "'.'", "'='", "'>'", "'<'", "'!'",
            "'~'", "'?'", "':'", "'=='", "'<='", "'>='", "'!='", "'&&'", "'||'",
            "'++'", "'--'", "'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", "'%'",
            "'+='", "'-='", "'*='", "'/='", "'&='", "'|='", "'^='", "'%='", "'<<='",
            "'>>='", "'>>>='", "'->'", "'::'", "'@'", "'...'"
    };


    private static void fileter(HashSet<String> proset) {
        for (int i = 0; i < internals.length; i++) {
            if (proset.contains(internals[i])) {
                proset.remove(internals[i]);
            } else if (internals[i] != null && proset.contains(internals[i].replace("'", ""))) {
                proset.remove(internals[i].replace("'", ""));
            }
        }
    }

    /**
     * 判断是否还有其他的产品代码修改
     *
     * @param adapter
     * @param proPath
     * @param proIndex
     * @param testIndex
     * @param commitMessages
     * @return true: 还有其他；false: 没有其他
     */
    public static boolean hasOtherPro(GitAdapter adapter, String proPath, int proIndex, int testIndex, List<CommitMessage> commitMessages) {
        // 0  ---->  Length
        // new ---->  old
        // test  <--- old
        for (int i = proIndex - 1; i >= testIndex; i--) {
            try {
                if (adapter.getDiffOfFileInCommit(commitMessages.get(i), proPath) != null) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean hasOtherTest(GitAdapter adapter, String testPath, int proIndex, int testIndex, List<CommitMessage> commitMessages) {
        // 0  ---->  Length
        // new ---->  old
        // test  <--- old
        for (int i = testIndex + 1; i <= proIndex; i++) {
            try {
                if (adapter.getDiffOfFileInCommit(commitMessages.get(i), testPath) != null) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 判断更改的粒度是否足够引起测试修改
     *
     * @param editScrips: 编辑操作
     * @return: True or False
     */
    public static boolean isRelated(final EditScript editScrips, RuleFilter ruleFilter) {
        //定义的过滤规则
        var tempScripts = new EditScript();

        editScrips.forEach(x -> tempScripts.add(x));

        ruleFilter.filter(tempScripts);

        return tempScripts.size() != 0;
    }

    /**
     * 获取Commit 在 commitMessage 中的索引位置
     *
     * @param queryCommit:    将要查询的Commit
     * @param commitMessages: Total commit messages
     * @return The index of queryCommit in the commitMessages
     */
    public static int getIndexCommit(String queryCommit, List<CommitMessage> commitMessages) {
        // 理论上 queryCommit 由于按照时间进行了排序 会导致比较早的开始处理，因此commitMessages从size=length 开始遍历查找
        for (int j = commitMessages.size() - 1; j >= 0; j--) {
            if (queryCommit.equalsIgnoreCase(commitMessages.get(j).getCommitId())) {
                return j;
            }
        }
        return -1;
    }


    public static ArrayList<String> getImportsTokens(GitAdapter gitAdapter, DiffEntry diffentry, String newContent, String oldContent, CommitMessage commitMessage) throws IOException {


        List<int[]> addLines = new ArrayList<>();
        List<int[]> delLines = new ArrayList<>();

        CodeDiff.getChangeLines(CodeDiff.getDiffFormatter(true, null, gitAdapter.getRepository()), diffentry, addLines, delLines);

        var addLinesLists = CodeDiff.translate(addLines);   //Tanslate changed lines

        var delLinesLists = CodeDiff.translate(delLines);  //Tanslate changed lines

        //TODO: 过滤Comment所在的行数

        addLinesLists = keepImports(addLinesLists, newContent);

        delLinesLists = keepImports(delLinesLists, oldContent);

//        addLinesLists.forEach(System.out::println);
//        delLinesLists.forEach(System.out::println);


        ArrayList<String> result = new ArrayList<>();

        addLinesLists.forEach(index -> {
            try {
                result.add(tokenize_imports(FileTool.getContent(newContent, index)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        delLinesLists.forEach(index -> {
            try {
                result.add(tokenize_imports(FileTool.getContent(oldContent, index)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return result;
    }

    private static List<Integer> keepImports(List<Integer> ChangedLinesLists, String Content) {
        var intervalTree = PareInstance.getImportsInterval(Content); //Comment的行
        return ChangedLinesLists.stream().filter(x -> {
            return intervalTree.query(x).size() != 0;   //当前的更改行在intervalTree中查询，如果size!=0代表查询到，即是impors修改
        }).collect(Collectors.toList());
    }


    public static String tokenize_imports(String content) {
        var transcontent = content.strip();
        String b[] = transcontent.split("\\W");
        return b[b.length - 1];
    }

    /**
     * filter the changes since the refactors.
     *
     * @param gitAdapter
     * @param add_changedLines add code lines
     * @param del_changedLines del code lines
     * @param AfterContent     the content of the new commit
     * @param BeforeContent    the content of the old commit
     * @param commitMessage
     * @param file_name        the file name
     * @return
     */
    public static Pair<List<Integer>, List<Integer>> removeRefactors(GitAdapter gitAdapter, List<Integer> add_changedLines, List<Integer> del_changedLines, String AfterContent, String BeforeContent,
                                                                     CommitMessage commitMessage, String file_name) throws Exception {

        var commitResult = RefDiffExtractor.scanRefactorings(gitAdapter, commitMessage);

        CodeFormatter findResult = null;
        for (int i = 0; i < commitResult.size(); i++) {
            if (commitResult.get(i).getAfterFileName().equalsIgnoreCase(file_name)) {
                findResult = commitResult.get(i);
                break;
            }
        }
        if (findResult == null) {   //没有Factoring
            return null;
        }

        var refactorings = findResult.getRefactorings();

        var aftercompilationUnit = PareInstance.initCompilationUnit(AfterContent);

        var beforecompilationUnit = PareInstance.initCompilationUnit(BeforeContent);

        IntervalTree<Integer> afterTree = new IntervalTree();

        IntervalTree<Integer> beforeTree = new IntervalTree();

        refactorings.forEach(x -> {
            afterTree.add(new IntegerInterval(
                    aftercompilationUnit.getLineNumber(x.getAfterBegin()), aftercompilationUnit.getLineNumber(x.getAfterEnd()), Interval.Bounded.CLOSED));
            beforeTree.add(new IntegerInterval(
                    beforecompilationUnit.getLineNumber(x.getBeforeBegin()), beforecompilationUnit.getLineNumber(x.getBeforeEnd()), Interval.Bounded.CLOSED));
        });
        //没有查询到: size==0 ==> has no refactorings.
        var add_result = add_changedLines.stream().filter(x -> afterTree.query(x).size() == 0).collect(Collectors.toList());
        var del_result = del_changedLines.stream().filter(x -> beforeTree.query(x).size() == 0).collect(Collectors.toList());
        return Pair.of(add_result, del_result);
    }

    /**
     * filter the changes since the refactors.
     *
     * @param gitAdapter
     * @param add_changedLines add code lines
     * @param del_changedLines del code lines
     * @param AfterContent     the content of the new commit
     * @param BeforeContent    the content of the old commit
     * @param file_name        the file name
     * @return
     */
    public static Pair<List<Integer>, List<Integer>> removeRefactors(GitAdapter gitAdapter, List<Integer> add_changedLines, List<Integer> del_changedLines, String AfterContent, String BeforeContent,
                                                                     CommitMessage commitMessage) throws Exception {

        var Refactorings = RefactoringMine.ScanRefactoringMine(BeforeContent, AfterContent);

        if (Refactorings == null) {   //没有Factoring
            return null;
        }

        IntervalTree<Integer> afterTree = new IntervalTree();

        IntervalTree<Integer> beforeTree = new IntervalTree();

        Refactorings.forEach(refactoring -> {
            var leftsides = refactoring.leftSide();
            var rightsides = refactoring.rightSide();
            leftsides.forEach(x -> {
                beforeTree.add(new IntegerInterval(x.getStartLine(), x.getEndLine(), Interval.Bounded.CLOSED));
            });
            rightsides.forEach(x -> {
                afterTree.add(new IntegerInterval(x.getStartLine(), x.getEndLine(), Interval.Bounded.CLOSED));
            });
        });

        //没有查询到: size==0 ==> has no refactorings.
        var add_result = add_changedLines.stream().filter(x -> afterTree.query(x).size() == 0).collect(Collectors.toList());
        var del_result = del_changedLines.stream().filter(x -> beforeTree.query(x).size() == 0).collect(Collectors.toList());
        return Pair.of(add_result, del_result);
    }

    /**
     * @param gitAdapter
     * @param add_changedLines
     * @param del_changedLines
     * @param AfterContent
     * @param BeforeContent
     * @param commitMessage
     * @param file_name
     * @return True: only Refactorings; False: Non only Refactorings.
     */
    public static boolean IsOnlyRefactor(GitAdapter gitAdapter, DiffEntry diffentry, String AfterContent, String BeforeContent,
                                         CommitMessage commitMessage, String file_name) {

        List<int[]> addLines = new ArrayList<>();
        List<int[]> delLines = new ArrayList<>();

        try {
            CodeDiff.getChangeLines(CodeDiff.getDiffFormatter(true, null, gitAdapter.getRepository()), diffentry, addLines, delLines);
        } catch (IOException e) {
            e.printStackTrace();
        }

        var addLinesLists = CodeDiff.translate(addLines);   //Tanslate changed lines

        var delLinesLists = CodeDiff.translate(delLines);  //Tanslate changed lines

        try {
            // var result=removeRefactors(gitAdapter,addLinesLists,delLinesLists,AfterContent,BeforeContent,commitMessage,file_name);
            var result = removeRefactors(gitAdapter, addLinesLists, delLinesLists, AfterContent, BeforeContent, commitMessage);
            if (result == null) {
                return false;
            } else {

                var add_result = result.getLeft();
                var del_result = result.getRight();
                if (add_result.size() != 0 || del_result.size() != 0) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 结算PorTokens 和 TestTokens中的最大的相似度
     *
     * @param proTokens
     * @param testTokens
     * @return
     */
    public static double maxSimilarity(final HashSet<String> proTokens1, final HashSet<String> testTokens1) {
        var temp_pro_Tokens = innerProcess(proTokens1);
        var temp_test_Tokens = innerProcess(testTokens1);
//        var temp_pro_Tokens=proTokens1;
//        var temp_test_Tokens=testTokens1;
        var proItrator = temp_pro_Tokens.iterator();
        double max_value = Double.MIN_VALUE;
        while (proItrator.hasNext()) {
            var proToken = proItrator.next();
            var testItrator = temp_test_Tokens.iterator();
            double max_value_iter = Double.MIN_VALUE;
//            System.out.println("Pro: "+proToken);
            while (testItrator.hasNext()) {
                var testToken = testItrator.next();
                double singleScore = LCS_local.MaxRatio(proToken.toLowerCase(Locale.ROOT), testToken.toLowerCase(Locale.ROOT));
                if (singleScore > max_value_iter) {
                    max_value_iter = singleScore;
                }
//                System.out.println(proToken+" "+testToken+" "+singleScore);
            }
            if (max_value_iter > max_value) {
                max_value = max_value_iter;
            }

        }
        return max_value;
    }

    private static HashSet<String> innerProcess(HashSet<String> Tokens) {
        var result = new HashSet<String>();
        var iter = Tokens.iterator();
        while (iter.hasNext()) {
            var value = iter.next();
            if (!value.strip().equalsIgnoreCase("")) {
                result.addAll(tokenize_identifier_raw(value));
            }
        }
        return result;
    }

    private static HashSet<String> tokenize_identifier_raw(String token) {
        var result = new HashSet<String>();
        var subtokens = token.split("'_+");
        for (String a : subtokens) {
            if (!a.strip().equals("")) {
                result.addAll(camel_case_split(a));
            }
        }
        return result;
    }

    private static HashSet<String> camel_case_split(String token) {
        var result = new HashSet<String>();
        var temp = token.replaceAll("([A-Z]+)", " $1");
        var subtokens = temp.replaceAll("([A-Z][a-z])", " $1").strip().split(" ");
        for (String a : subtokens) {
            if (!a.strip().equalsIgnoreCase("")) {
                result.add(a);
            }
        }
        return result;
    }
}


