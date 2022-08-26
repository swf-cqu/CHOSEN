package Tool;

import Bean.FeatureDo;
import Persistent.Serialization;
import com.github.gumtreediff.tree.Tree;

import java.io.*;
import java.util.HashSet;
import java.util.List;

public class FileTool {
    public static String getContent(String content, int n) throws IOException {

        StringReader reader=new StringReader(content);

        BufferedReader br = new BufferedReader(reader);
        for(int i = 0; i < n-1; ++i)
            br.readLine();
        return br.readLine();
    }

    /**
     * 获取每个EditScrip Action的Label标签
     * @param node
     */
    public static void getLabel(Tree node, HashSet<String> result){
        if(node.hasLabel()){
            result.add(node.getLabel());
        }

        if(node.isLeaf()){
            return;
        }
        if(!node.isLeaf()){
            List<Tree> trees=node.getChildren();
            trees.forEach(x-> getLabel(x,result));
        }
    }

    public static double GetTokenRelated(HashSet<String> proset, HashSet<String> testset){
        if(proset.size()==0 && testset.size()==0){
            return 0;
        }
        HashSet<String> interSet=new HashSet<>();
        interSet.addAll(proset);
        interSet.retainAll(testset);
        return 2.0 * interSet.size() / (proset.size()+testset.size());
    }

    /**
     * Obtain the FeatureDo according to the File
     *
     * @param file: file name??
     * @throws IOException
     * @return: file -->  FeatureDo
     */
    public static FeatureDo getFeatureDo(File file) throws IOException {
        assert file.exists();
        char[] content = new char[(int) file.length()];
        try (FileReader reader = new FileReader(file)) {
            reader.read(content);
        }
        return Serialization.json2Bean(new String(content), FeatureDo.class);
    }

}
