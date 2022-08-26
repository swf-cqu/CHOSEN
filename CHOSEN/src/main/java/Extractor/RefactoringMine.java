package Extractor;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class RefactoringMine {
    public static List<Refactoring> ScanRefactoringMine(Repository repo, String startcommit, String endcommit) throws Exception {
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        List<org.refactoringminer.api.Refactoring> refactorings = null;
        miner.detectBetweenCommits(repo, startcommit,
                endcommit, new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<org.refactoringminer.api.Refactoring> refactorings) {
                        System.out.println("Refactorings at " + commitId);
                        for (Refactoring ref : refactorings) {
                            System.out.println(ref.toString());
                        }
                    }
                });
        return refactorings;
    }

    synchronized public static List<Refactoring> ScanRefactoringMine(String srcContent, String dstContent) throws Exception {
        File srcFile = null;
        File dstFile = null;
        File src_dir = new File("E:/Project_Code/JustInTestPro/srcTemp/");
        File dst_dir = new File("E:/Project_Code/JustInTestPro/dstTemp/");
        if (src_dir.exists()) {
            FileUtils.deleteDirectory(src_dir);
        }
        if (dst_dir.exists()) {
            FileUtils.deleteDirectory(dst_dir);
        }
        src_dir.mkdirs();
        dst_dir.mkdirs();
        srcFile = new File(src_dir.getAbsolutePath() + "/" + "srcFile" + ".java");
        dstFile = new File(dst_dir.getAbsolutePath() + "/" + "dstFile" + ".java");
        try (FileWriter fsrcout = new FileWriter(srcFile);
             PrintWriter srcout = new PrintWriter(fsrcout);
             FileWriter fdstout = new FileWriter(dstFile);
             PrintWriter dstout = new PrintWriter(fdstout)) {
            srcout.println(srcContent);
            dstout.println(dstContent);
            UMLModel model1 = new UMLModelASTReader(src_dir).getUmlModel();
            UMLModel model2 = new UMLModelASTReader(dst_dir).getUmlModel();
            UMLModelDiff modelDiff = model1.diff(model2);
            List<Refactoring> refactorings = modelDiff.getRefactorings();
            return refactorings;
        } catch (IOException e1) {
            return null;
        }
    }

    synchronized public static List<Refactoring> ScanRefactoringMine(File srcFile, File dstFile) throws IOException, RefactoringMinerTimedOutException {
        // input data
        System.out.println(srcFile.getPath() + " " + dstFile.getPath());
        UMLModel model1 = new UMLModelASTReader(srcFile).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(dstFile).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        System.out.println("refactorings size " + refactorings.size());
        return refactorings;
    }


}
