package Utility;

import Action.CodeDiff;
import Action.GitAdapter;
import Obj.ClassInfo;
import Obj.CommitMessage;
import Persistent.Serialization;
import Resource.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;


public class ProcedureUtility {
    /**
     * Through the file to obtain the commits of project which has been stored into file.
     * @param commits_file
     * @return
     */
    public static List<CommitMessage> getAllCommits(File commits_file) {
        var list=new ArrayList<CommitMessage>();
        try {
            var reader=new BufferedReader(new FileReader(commits_file));
            String line;
            while((line=reader.readLine())!=null){
                CommitMessage commitMessage= Serialization.json2Bean(line, CommitMessage.class);
                list.add(commitMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     *将所有的历史的文件信息进行保存,若存在改文件则进行文件读取获取文件列表
     * @param commitments 历史Commits
     * @param projectName 项目的名称
     * @return 返回所有的历史文件list
     */

    public static ArrayList<String> getAllFiles(GitAdapter gitAdapter, List<CommitMessage> commitments, String projectName) throws IOException {
        File fileDirectory=new File(Resource.allFilesDirctory);
        if(!fileDirectory.exists()){
            fileDirectory.mkdir();
        }
        System.out.println(fileDirectory.getAbsoluteFile());
        File file =new File(fileDirectory.getPath()+File.separator+projectName+".csv");
        var uniqueList=new ArrayList<String>();
        var uniqueSet=new HashSet<String>();
        if(file.exists()){
            var reader=new BufferedReader(new FileReader(file));
            String line;
            while((line=reader.readLine())!=null){
                uniqueList.add(line);
            }
        }else{
            for(CommitMessage commitMessage:commitments){
                var list=gitAdapter.getJavaFilesCommit(commitMessage.getCommitId());
                for(var fileName:list){
                    uniqueSet.add(fileName);
                }
            }
            var writer=new BufferedWriter(new FileWriter(file));
            uniqueSet.forEach((file_name)-> {
                try {
                    uniqueList.add(file_name);
                    writer.write(file_name+"\r\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writer.close();
        }
        return uniqueList;
    }

    /** 用来处理Commits列表，获取每次Commit的内容变化文件；
     * @param gitAdapter :adapter示例
     * @param commitments : 存放是所有的Commits列表*/
    private static void ProcessCommits(GitAdapter gitAdapter, List<CommitMessage> commitments) {
        File pFile=new File("./"+gitAdapter.getProjectName()+"_Diffs");
        try {
            FileUtils.deleteDirectory(pFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        pFile.mkdirs();
        File uFile=new File("./"+gitAdapter.getProjectName()+"_NoDiffs.csv");
        if(uFile.exists()){uFile.delete();}
        try {
            uFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
        for(CommitMessage message:commitments){
            if(message.getLastCommitId()==null){
                continue;
            }
            try {
                List<DiffEntry> diffs= (List<DiffEntry>) CodeDiff.diffCommitToCommit(gitAdapter,message.getCommitId(),message.getLastCommitId(),false)[1];
                List<DiffEntry> testDiff=new ArrayList<>();
                boolean isHaveTest=false;
                for(DiffEntry entry:diffs){
                    if(entry.getNewPath().contains(".java")){
                        testDiff.add(entry);
                    }
                    if(entry.getNewPath().contains("Test")){
                        isHaveTest=true;
                    }
                }
                //note：entry.getNewID获取的是改变的文件内容的SHA1
                if(isHaveTest){
                    File file=new File(pFile.getName()+File.separator+dateFormat.format(message.getDate())+".csv");
                    BufferedWriter writer=new BufferedWriter(new FileWriter(file));
                    for(DiffEntry entry:testDiff){
                        writer.write(message.getCommitId()+" "+entry.getChangeType()+" "+entry.getNewPath()+"\r\n");
                    }
                    writer.close();
                }else{
                    BufferedWriter writer=new BufferedWriter(new FileWriter(uFile, true));
                    writer.write(message.getCommitId()+"\r\n");
                    writer.close();
                }
            } catch (IOException | GitAPIException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * write all commits into the file.
     * @param commitments  //存放的是所有的Commits列表
     * @param file  //存放文件
     */
    public static void WriteCommits(List<CommitMessage> commitments, File file) {
        try {
            if(!file.exists()){
                file.createNewFile();
            }else{
                return ;
            }
            BufferedWriter writer=new BufferedWriter(new FileWriter(file));
            for(CommitMessage commitmessage:commitments){
                writer.write(Serialization.ObjToJSON(commitmessage)+"\r\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对测试用例进行产片代码的绑定
     * @param adapter
     * @param commitment
     */
    public static List<ClassInfo> match(GitAdapter adapter, CommitMessage commitment,boolean isIgnore) throws Exception {
            //先获得针对这一次Commit的文件
        if(commitment.getCommitId()==null|| commitment.getLastCommitId()==null){
               return null;
            }
            //获取的类信息可能存在没有改变信息的情况（由于忽略了空白字符的原因）

        return CodeDiff.diff2Commit_concrete(adapter, commitment.getCommitId(), commitment.getLastCommitId(),isIgnore);

    }

    /**
     *是否忽略空白字符的Diff条目
     * @param adapter
     * @param commitments 历史提交信息
     * @param isIgnore 是否忽略空白字符
     * @param regex 过滤的文件
     * @return
     */
    public static List<ImmutablePair<DiffEntry,String>> getCodesByFilter(GitAdapter adapter, List<CommitMessage> commitments, boolean isIgnore, String regex) throws Exception {
        var result = new ArrayList<ImmutablePair<DiffEntry, String>>();

        for (CommitMessage commitMessage : commitments) {

            if (commitMessage.getCommitId() == null || commitMessage.getLastCommitId() == null) {
                continue;
            }

            Object[] diff_result = CodeDiff.diffCommitToCommit(adapter,commitMessage.getCommitId(), commitMessage.getLastCommitId(), isIgnore);
            List<DiffEntry> diffs = (List<DiffEntry>) diff_result[1];
            DiffFormatter formatter = (DiffFormatter) diff_result[0];
            for (DiffEntry diffEntry : diffs) {
                //            System.out.println(diffEntry.getChangeType());
                if (formatter.toFileHeader(diffEntry).toEditList().size() != 0) {
                    if (diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE && Pattern.matches(regex, diffEntry.getOldPath())) {
                        var content = adapter.getCommitSpecificFileContent(commitMessage.getLastCommitId(), diffEntry.getOldPath());
                        result.add(new ImmutablePair<>(diffEntry, content));
                    } else if (Pattern.matches(regex, diffEntry.getNewPath())) {
                        var content = adapter.getCommitSpecificFileContent(commitMessage.getCommitId(), diffEntry.getNewPath());
                        result.add(new ImmutablePair<>(diffEntry, content));
                    }
                }
            }
        }
        return result;
    }

    /**
     * 将文件写入目录中
     * @param file 将要写入内容的文件
     * @param testClass 测试文件
     * @param production_list 在同一个Commit的产品代码列表
     */
    public static void WriteClassInfo(File file,ClassInfo testClass, ArrayList<ClassInfo> production_list) throws IOException {
        if(file.exists()){
            file =new File((file.getPath().substring(0,file.getPath().indexOf(".json"))+"_"+(new Random().nextInt(100000)+".json")));
        }
        BufferedWriter writer=new BufferedWriter(new FileWriter(file,false));
        writer.write(Serialization.ObjToJSON(testClass)+"\r\n");
        if(production_list!=null){
            for(ClassInfo info:production_list){
                writer.write(Serialization.ObjToJSON(info)+"\r\n");
            }
        }
        writer.close();
    }
}
