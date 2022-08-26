package SimilarityTool;

import java.io.*;
import java.util.HashMap;

public class CalclulationDemo2 {
    public  static  void main(String args[]){
//        FastText model = FastText.Companion.loadCppModel(new File("./WordSim/src/main/resources/wiki-news-300d-1M.vec"));
//        var vector1=model.getWordVector("normalize");
//        var vector2=model.getWordVector("normalization");
//        System.out.print(vector1+" "+vector2);
        HashMap vertor = new HashMap<String,int[]>();
        File file=new File("./WordSim/src/main/resources/wiki-news-300d-1M.vec");
        try(BufferedReader bufferedReader=new BufferedReader(new FileReader(file))){
            bufferedReader.readLine();
            String single=null;
            while((single=bufferedReader.readLine())!=null){
                var tokens=single.split(" ");
                double vectors[]=new double[300];
                for(int i=1;i<tokens.length;i++){
                    vectors[i-1]=Double.parseDouble(tokens[i]);
                }
                vertor.put(tokens[0],vectors);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        var v1=(double[])vertor.get("public");
        var v2=(double[])vertor.get("private");
        double sum=0.0;
        double v1_sq=0.0;
        double v2_sq=0.0;
        for(int i=0;i<v1.length;i++) {
            sum+=v1[i]*v2[i];
            v1_sq+=v1[i]*v1[i];
            v2_sq+=v2[i]*v2[i];
        }
        System.out.println(sum/(Math.sqrt(v1_sq)*Math.sqrt(v2_sq)));
    }
}
