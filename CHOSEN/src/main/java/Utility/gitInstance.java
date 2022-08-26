package Utility;

import Action.GitAdapter;

import java.util.ResourceBundle;

/**
 * @author SWF
 * @Date 2021/3/20
 * 读取GIt信息
 */
public class gitInstance {
    /**
     * 根据信息获取一个Git示例
     *
     * @param resourceName：存放资源文件
     * @return：获取的示例
     */

    private static GitAdapter ReadInformation(String resourceName) {
        ResourceBundle res = ResourceBundle.getBundle(resourceName);
        String remotePath = res.getString("RemoteGit");
        String localPath = res.getString("LocalGit");
        String branchName = res.getString("branchName");
        String projectName = res.getString("projectName");

        String filePath = localPath + "/" + projectName; //存放目录加项目名称

        return adapter = new GitAdapter(remotePath, filePath, branchName);
    }

    private static GitAdapter adapter = null;

    public static GitAdapter get(String resourceName) {

        return adapter = ReadInformation(resourceName);


    }

    public static GitAdapter get(String projectName, String branchName) {

        return adapter = ReadInformation(projectName, branchName);

    }

    private static GitAdapter ReadInformation(String projectName, String branchName) {

        var remotePath = "https://github.com/apache/" + projectName + ".git";
        var localPath = "E:/Data/emperical_study(Co_evolution)";


        String filePath = localPath + "/" + projectName; //存放目录加项目名称

        return adapter = new GitAdapter(remotePath, filePath, branchName);
    }
}
