package gitlet;
import java.io.Serializable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.ArrayList;


public class Branch implements Serializable {
    //Name of branch.
    protected String branchName;
    //Id of head commit.
    protected String headCommitId;
    //Hashmap of contents.
    protected HashMap<String, String> branchContents = new HashMap<>();
    //List of commits of branch.
    protected ArrayList<String> branchCommits = new ArrayList<>();
    //Id of branch split.
    protected String splitCommit;


    protected Branch(String name) {
        branchName = name;
    }

    protected Branch(String name, String id, HashMap<String, String> map) {
        branchName = name;
        headCommitId = id;
        splitCommit = id;
        branchContents.putAll(map);
        this.loadCommits();


    }

    //Serializes branch.
    protected void saveBranch() {
        try {
            Branch obj = this;
            File outfile = getBranchFolder(branchName);
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outfile));
            out.writeObject(obj);
            out.close();
        } catch (IOException e) {
            System.out.println("Error in branch serialization");
            return;
        }


    }

    //Loads saved branch.
    protected static Branch load(String name) throws IllegalArgumentException {

        try {
            File tempStaging = Utils.join(Repo.directory, ".gitlet/", "branches/", name);
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(tempStaging));
            Branch result = (Branch) in.readObject();
            in.close();
            return result;

        } catch (IOException e) {
            throw new IllegalArgumentException();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException();
        }

    }

    //Updates previous commits on creation.
    protected void loadCommits() {
        String temp = this.headCommitId;
        while (temp != null) {

            Commit head = Commit.load(temp);
            this.branchCommits.add(temp);
            temp = head.getParent();
            head.finalize();
        }
    }

    //Returns filepath for branch object.
    protected File getBranchFolder(String name) {
        String[] path = {".gitlet/", "branches/", name};
        return Utils.join(Repo.directory, path);
    }

    //Overwrites current branch with given branch.
    protected void overwriteBranch(String b) {
        Branch temp = load(b);
        this.headCommitId = temp.headCommitId;
        this.branchContents = temp.branchContents;
        this.branchCommits = temp.branchCommits;
        temp.saveBranch();
    }


}
