package Extractor;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Objects;

public class CodeFormatter {
    @SerializedName("refactorings")
    ArrayList<Refactoring> refactorings=null;
    @SerializedName("CommitID")
    String CommitID;
    @SerializedName("BeforeFileName")
    String BeforeFileName;
    @SerializedName("AfterFileName")
    String AfterFileName;


    public CodeFormatter(){
        refactorings=new ArrayList<>();
    }
    public ArrayList<Refactoring> getRefactorings() {
        return refactorings;
    }

    public void setRefactorings(ArrayList<Refactoring> refactorings) {
        this.refactorings = refactorings;
    }

    public String getCommitID() {
        return CommitID;
    }

    public void setCommitID(String commitID) {
        CommitID = commitID;
    }

    public String getBeforeFileName() {
        return BeforeFileName;
    }

    public void setBeforeFileName(String beforeFileName) {
        BeforeFileName = beforeFileName;
    }

    public String getAfterFileName() {
        return AfterFileName;
    }

    public void setAfterFileName(String afterFileName) {
        AfterFileName = afterFileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeFormatter that = (CodeFormatter) o;
        return CommitID.equals(that.CommitID) && BeforeFileName.equals(that.BeforeFileName) && AfterFileName.equals(that.AfterFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(CommitID, BeforeFileName, AfterFileName);
    }
}
