
package gitlet;
import java.io.File;

public class Repo {

    //Working directory.
    static String directory = System.getProperty("user.dir") + "/";

    //Initializes repo in current directory.
    void init() {
        File pwd = new File(directory);
        File gitletDir = new File(pwd, ".gitlet");
        gitletDir.mkdir();


        File pwd2 = new File(directory + ".gitlet/");
        File gitletDir2 = new File(pwd2, "commits");
        gitletDir2.mkdir();

        File pwd3 = new File(directory + ".gitlet/");
        File gitletDir3 = new File(pwd3, "branches");
        gitletDir3.mkdir();




    }

    //Returns directory of repo.
    public static String getDir() {
        return directory;
    }

    //Sets staging area, initial commit, and Master branch in repo.
    protected void setStage() {

        Staging stagingArea;
        stagingArea = new Staging(directory);

        Commit initComm = new Commit();
        stagingArea.headCommit = initComm.getID();
        stagingArea.allCommits.add(initComm.getID());
        Branch master = new Branch("master");
        initComm.finalize();

        master.headCommitId = stagingArea.headCommit;

        master.saveBranch();

        stagingArea.init();

    }


}
