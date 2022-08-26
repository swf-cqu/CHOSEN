package Action;


import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;

import java.io.*;

/**
 * @author ZLL
 */
public class GumTreeAdatpter {


    private static GumTreeAdatpter instance;

    public static GumTreeAdatpter getInstance(){
        if(instance==null){
            Run.initClients();
            instance=new GumTreeAdatpter();
        }
        return instance;
    }

    private GumTreeAdatpter(){

    }

    /**返回的Operation可以具体获得所操作的节点 从而进行分析；
     * 根据前后的Java内容来得到两者之间的细粒度的查编辑操作
     * @param NewContent Left的Java内容
     * @param OldContent Right的Java内容
     * @return
     */
    public EditScript Diff2File(String NewContent, String OldContent){
//        AstComparator astComparator=new AstComparator();
//        //获取两者之间的差异
//        Diff diff=astComparator.compare(OldContent,NewContent);
//        return diff;
        var edscripts=GetActions(OldContent,NewContent);
        return edscripts;
    }


//    public List<Pair<String,String>> getChangeLines(List<Operation> operations, boolean IsSrc){
//        List list=null;
//        if(IsSrc){
//            list=operations.stream()
//                    .map((Operation operation)->{return new Pair(operation.getAction().getName(),operation.toString());})
//                    .collect(Collectors.toList());
//
//        }else{
//            list=operations.stream()
//                    .map((Operation operation)->{return new Pair(operation.getAction().getName(),operation.toString());})
//                    .collect(Collectors.toList());
//        }
//
//        return list;
//
//    }


//    /**
//     * Reader, String File.class 类型
//     * @param NewReader 新版本的对象
//     * @param OldReader 旧版本的对象
//     * @return
//     */
//    public List<Action>  Diff2File(Object NewReader, Object OldReader){
//        //根据对象进行映射
//        ITree rootSpoonLeft = null,rootSpoonRight=null;
//        List<Action> actions=null;
//        if(NewReader.getClass()!=OldReader.getClass()){
//            return null;
//        }
//        try {
//            if(NewReader.getClass()==Reader.class){
//                Reader newReader=(Reader)NewReader;
//                Reader oldReader=(Reader)OldReader;
//                rootSpoonRight=new JdtTreeGenerator().generateFromReader(newReader).getRoot();
//                rootSpoonLeft=new JdtTreeGenerator().generateFromReader(oldReader).getRoot();
//
//            }else if(NewReader.getClass()==String.class){
//                String newReader=(String)NewReader;
//                String oldReader=(String)OldReader;
//                rootSpoonRight=new JdtTreeGenerator().generateFromString(newReader).getRoot();
//                rootSpoonLeft=new JdtTreeGenerator().generateFromString(oldReader).getRoot();
//            }else if(NewReader.getClass()==File.class) {
//                File newReader = (File) NewReader;
//                File oldReader = (File) OldReader;
//                rootSpoonRight = new JdtTreeGenerator().generateFromFile(newReader).getRoot();
//                rootSpoonLeft = new JdtTreeGenerator().generateFromFile(oldReader).getRoot();
//            }
//            final MappingStore mappingsComp = new MappingStore();
//           //CompositeMatchers.CompleteGumtreeMatcher  匹配所有的更改
//            //CompositeMatchers.ClassicGumtree   //忽略普通注释
//            final Matcher matcher = new CompositeMatchers.ClassicGumtree(rootSpoonLeft, rootSpoonRight, mappingsComp);
//            matcher.match();
//
//            final ActionGenerator actionGenerator = new ActionGenerator(rootSpoonLeft, rootSpoonRight,
//                    matcher.getMappings());
//            actionGenerator.generate();
//            actions = actionGenerator.getActions();
////            for(Action act : actions){
////                System.out.println("Action: {}"+act.toString());
////                System.out.println("Name: {} - {} - {}"+act.getName()+act.getClass()+act.getNode());
////                System.out.println("Node: {} - {} - {}"+ act.getNode().getLength()+act.getNode().getPos()+act.getNode().getParent());
////                logger.info("lineNo: {} - {}", PatchParserUtil.getLineNumber(dstFile, act.getNode().getPos()),
////                        PatchParserUtil.getLineNumber(dstFile, act.getNode().getPos() + act.getNode().getLength()));
////                logger.info("lineNo: {} - {}", PatchParserUtil.getLineNumberFromCU(dstFile, act.getNode().getPos()),
////                        PatchParserUtil.getLineNumberFromCU(dstFile, act.getNode().getPos() + act.getNode().getLength()));
////            }
//            return actions;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
////                    System.out.println(act.getName()+" "+act.getClass()+" "+act.getNode().getLabel()+" "+act.getNode().getPos());
////            logger.info("Action: {}", act.toString());
////            logger.info("Name: {} - {} - {}", act.getName(), act.getClass(), act.getNode());
////            logger.info("Node: {} - {} - {}", act.getNode().getLength(), act.getNode().getPos(), act.getNode().getParent());
////            logger.info("lineNo: {} - {}", PatchParserUtil.getLineNumber(dstFile, act.getNode().getPos()),
////                    PatchParserUtil.getLineNumber(dstFile, act.getNode().getPos() + act.getNode().getLength()));
////            logger.info("lineNo: {} - {}", PatchParserUtil.getLineNumberFromCU(dstFile, act.getNode().getPos()),
////                    PatchParserUtil.getLineNumberFromCU(dstFile, act.getNode().getPos() + act.getNode().getLength()));
////        }
//
//        return  actions;
//    }


    /**
     * Obtain the Edit Action between two files
     * @param srcFile: oldFile
     * @param dstFile: NewFile
     * @return：获取的EditActions 实例
     */
    synchronized public EditScript GetActions(File srcFile, File dstFile) {
        // input data
        Run.initGenerators();
        Tree src;
        Tree dst;
        try {
            // parse AST trees
            // -- retrieves and applies the default parser for the file
//            System.out.println(getFeatureDo(srcFile)+" "+getFeatureDo(dstFile));
            src = TreeGenerators.getInstance().getTree(srcFile.getPath()).getRoot();
            dst = TreeGenerators.getInstance().getTree(dstFile.getPath()).getRoot();

//            src = new JavaParserGenerator().generateFrom().file(srcFile.getPath()).getRoot();
//            dst = new JavaParserGenerator().generateFrom().file(dstFile.getPath()).getRoot();

            // -- retrieves the default matcher
            Matcher defaultMatcher = Matchers.getInstance().getMatcher();
            // -- computes the mappings between the trees
            MappingStore mappings = defaultMatcher.match(src, dst);
            // -- instantiates the simplified Chawathe script generator
            EditScriptGenerator eSG = new SimplifiedChawatheScriptGenerator();
            // -- computes the edit script
            EditScript actions = eSG.computeActions(mappings);
            // NS: this goes beyond the example, I am just trying to get any human-readable output
//            for (Action a : actions) {
//                System.out.println(a.getNode().getType().toString());
//                System.out.println(a.toString());
//            }

            return actions;

        } catch (Exception treeGenerationException) {
            return null;
        }

    }

    synchronized public EditScript GetActions(String srcContent, String dstContent) {
        //在tempPath路径下创建临时文件"mytempfileXXXX.tmp"
        //XXXX 是系统自动产生的随机数, tempPath对应的路径应事先存在
        File srcFile = null;
        File dstFile = null;
        EditScript actions=null;
        try {
            srcFile = File.createTempFile("srcFile", ".java", new File("E:/Project_Code/JustInTestPro/Temp/"));
            dstFile = File.createTempFile("dstFile", ".java", new File("E:/Project_Code/JustInTestPro/Temp/"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileWriter fsrcout = new FileWriter(srcFile);
             PrintWriter srcout = new PrintWriter(fsrcout);
             FileWriter fdstout=new FileWriter(dstFile);
             PrintWriter dstout=new PrintWriter(fdstout)) {
             srcout.println(srcContent);
             dstout.println(dstContent);
             srcout.flush();
             dstout.flush();
             actions=GetActions(srcFile,dstFile);
        } catch (IOException e1) {
            System.out.println(e1);
        }

        srcFile.deleteOnExit();
        dstFile.deleteOnExit();
        return actions;
    }

    /**
     * Obtain the FeatureDo according to the File
     * @param file: file name??
     * @return: file -->  FeatureDo
     * @throws IOException
     */
    public static String getFeatureDo(File file) throws IOException {
        assert file.exists();
        char[] content=new char[(int)file.length()];
        try(FileReader reader=new FileReader(file)){
            reader.read(content);
        }
        return new String(content);
    }

}
