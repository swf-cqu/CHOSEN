package Extractor;

import Action.CodeDiff;
import Action.GitAdapter;
import Obj.CommitMessage;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import refdiff.core.RefDiff;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RefDiffExtractor {
	private static LanguagePlugin plugin;

	public static List<CodeFormatter>  scanRefactorings(GitAdapter adapter, CommitMessage commitMessage){

		var tempFolder = new File(adapter.getLocalPath()); //存放的reposity的文件路径

		plugin = new JavaPlugin(tempFolder);    //Java 插件

		try {
			return scanRefactorings(adapter,commitMessage.getLastCommitId(),commitMessage.getCommitId());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	private static List<CodeFormatter> scanRefactorings(GitAdapter adapter,String commitBefore, String commitAfter) throws Exception {
		try {

			var repo = adapter.getRepository();

			var rw = new RevWalk(repo);

			RevCommit revBefore = rw.parseCommit(repo.resolve(commitBefore));   //Commit提交

			RevCommit revAfter = rw.parseCommit(repo.resolve(commitAfter)); //Commit提交

			return scanRefactorings(adapter,revBefore,revAfter);

		} catch (Exception e) {
			//System.err.println("Error on get content from commits: "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private static List<CodeFormatter> scanRefactorings(GitAdapter adapter,RevCommit revBefore, RevCommit revAfter) throws Exception {
		List<CodeFormatter> refactorings=new ArrayList<>();
		try {
			var refDiff = new RefDiff(plugin);

			var repo = adapter.getRepository();


			CstDiff diff = refDiff.computeDiffBetweenRevisions(repo, revBefore, revAfter);   //Comput

			refactorings = getRefactorings(repo, diff, revBefore, revAfter);

			return refactorings;

		} catch (Exception e) {
			//System.err.println("Error on get content from commits: "+e.getMessage());
			e.printStackTrace();
		}
		return refactorings;
	}

	/**
	 * 获取针对Commit path 下的FileCode
	 * @param repo
	 * @param commit
	 * @param path
	 * @return
	 * @throws IOException
	 */
	private static String getFileCode(Repository repo, RevCommit commit, String path) throws IOException {
		try (ObjectReader reader = repo.newObjectReader(); RevWalk walk = new RevWalk(reader)) {
			RevTree tree = commit.getTree();
			TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

			if (treewalk != null) {
				byte[] content = reader.open(treewalk.getObjectId(0)).getBytes();
				return new String(content, "utf-8");
			} else {
				throw new FileNotFoundException(path);
			}
		}
	}

	/**
	 * 对于同一个File文件下的Refactorings 放到同一个CodeFormatter
	 * @param repo
	 * @param diff CodeRefactoring Diffs
	 * @param revBefore CommitBefore
	 * @param revAfter  CommitAfter
	 * @return 在CommitNew 下的 CodeFormatter
	 * @throws Exception
	 */

	public static List<CodeFormatter> getRefactorings(Repository repo, CstDiff diff, RevCommit revBefore, RevCommit revAfter) throws Exception {
		List<CodeFormatter> CodeFormatters = new ArrayList<>();
		for (Relationship rel : diff.getRefactoringRelationships()) {

			//生成新的CodeFormatter对象
			CodeFormatter codeFormatter=new CodeFormatter();
			codeFormatter.setCommitID(revAfter.getId().getName());  //Set the CommitID
			codeFormatter.setBeforeFileName(rel.getNodeBefore().getLocation().getFile());
			codeFormatter.setAfterFileName(rel.getNodeAfter().getLocation().getFile());
			//设置Refactorings
			Refactoring refactoring = Refactoring.FromRelationship(rel); // node before here
			fillRefactoringDiff(repo, revBefore, revAfter, refactoring,codeFormatter); // 对Refactoring进行补充
			codeFormatter.getRefactorings().add(refactoring);

			//新生成的CodeFormatter是否存在？
			if(CodeFormatters.contains(codeFormatter)){
				int index=CodeFormatters.indexOf(codeFormatter); //获取索引
				var BeforeRefactor=CodeFormatters.get(index);  //得到对象
				BeforeRefactor.getRefactorings().addAll(codeFormatter.getRefactorings());  //针对同一个File，添加新的Refactorings;
			}else{
				CodeFormatters.add(codeFormatter);
			}

		}

		return CodeFormatters;
	}


	/**
	 * RawTest Diffs
	 * @param repo
	 * @param revBefore
	 * @param revAfter
	 * @param refactoring
	 * @param codeFormatter
	 * @throws Exception
	 */


	private static void fillRefactoringDiff(Repository repo, RevCommit revBefore, RevCommit revAfter, Refactoring refactoring, CodeFormatter codeFormatter) throws Exception {
		String beforeCode = getFileCode(repo, revBefore, codeFormatter.getBeforeFileName()).substring(refactoring.getBeforeBegin(), refactoring.getBeforeEnd());
		String afterCode = getFileCode(repo, revAfter, codeFormatter.getAfterFileName()).substring(refactoring.getAfterBegin(), refactoring.getAfterEnd());

//		String advance=getFileCode(repo, revBefore, refactoring.getBeforeFileName()).substring(0,refactoring.getBeforeBegin());
//		String after= getFileCode(repo, revAfter, refactoring.getAfterFileName()).substring(0,refactoring.getAfterBegin());
//
//		System.out.println("Before: "+"  "+CountChar(advance,"\n")+" "+CountChar(beforeCode, "\n"));
//
//		System.out.println("After: "+"  "+CountChar(after,"\n")+" "+CountChar(afterCode, "\n"));

		refactoring.setDiff(CodeDiff.getDiff(beforeCode, afterCode));
	}

	private int CountChar(String str, String substr){
		int count=0;
		int i=0;
		while(str.indexOf(substr,i)>=0){
			count++;
			i=str.indexOf(substr,i)+substr.length();
		}
		return count;
	}



	private void printRefactorings(CstDiff diff) {
		if (diff.getRefactoringRelationships().size() > 0) {
			System.out.println("\nREFACTORINGS");
			System.out.println("-------------");
			for (Relationship rel : diff.getRefactoringRelationships()) {
				System.out.println(Refactoring.FromRelationship(rel));
			}
			System.out.println("-------------");
		}
	}
}