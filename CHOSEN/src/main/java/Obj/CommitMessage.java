package Obj;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommitMessage implements Comparable{
    /**
     * 获取Commit的ID
     */
    private String commitId;
    /**
     * 获取Commit的作者姓名
     */
    private String commitIdent;
    /**
     * 获取提交的信息
     */
    private String commitMessage;
    /**
     * 获取提交的日期
     */
     private String commitDate;
    /**
     * 获取此节点的上一个节点的CommitID
     */
    private String lastCommitId;
    /**
     * 获取此节点的上一个合并父亲节点
     */
    private String mergeBranchCommitId;

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getCommitIdent() {
        return commitIdent;
    }

    public void setCommitIdent(String commitIdent) {
        this.commitIdent = commitIdent;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(String commitDate) {
        this.commitDate = commitDate;
    }

    public String getLastCommitId() {
        return lastCommitId;
    }

    public void setLastCommitId(String lastCommitId) {
        this.lastCommitId = lastCommitId;
    }

    public String getMergeBranchCommitId() {
        return mergeBranchCommitId;
    }

    public void setMergeBranchCommitId(String mergeBranchCommitId) {
        this.mergeBranchCommitId = mergeBranchCommitId;
    }

    @Override
    public String toString() {
        return "CommitMessage{" +
                "commitId='" + commitId + '\'' +
                ", commitIdent='" + commitIdent + '\'' +
                ", commitMessage='" + commitMessage + '\'' +
                ", commitDate='" + commitDate + '\'' +
                ", lastCommitId='" + lastCommitId + '\'' +
                ", mergeBranchCommitId='" + mergeBranchCommitId + '\'' +
                '}';
    }

    /**
     *
     * @return 返回CommitDate 的Date形式
     * @throws ParseException
     */
    @JsonIgnore
    public Date getDate() throws ParseException {
        SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.parse(this.commitDate);
    }


    @Override
    public int compareTo(Object o) {
        if(o instanceof CommitMessage){
            return this.toString().compareTo(((CommitMessage)o).toString());
        }else{
            return -2;
        }
    }
}
