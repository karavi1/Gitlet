
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
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


class Staging implements Serializable {


    //Filepath for staging area.
    private File stagingAreaFile;
    //Head commit.
    protected String headCommit;
    //Contents of last commit.
    private HashMap<String, String> parentMap = new HashMap<>();
    //Staging area.
    private HashMap<String, String> currMap = new HashMap<>();
    //Staging for removals.
    private HashMap<String, String> removeMap = new HashMap<>();
    //List of all branches made.
    private ArrayList<String> allBranchNames = new ArrayList<>();
    //List of all commits made.
    protected ArrayList<String> allCommits = new ArrayList<>();
    //Current active branch.
    private String activeBranch = "master";

    protected Staging(String directory) {

        String[] path = {"/.gitlet", "/staging"};
        stagingAreaFile = Utils.join(directory, path);
        allBranchNames.add("master");

    }

    //Initializes and serializes staging area.
    void init() {
        try {
            Staging obj = this;
            File outfile = stagingAreaFile;
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outfile));
            out.writeObject(obj);
            out.close();
        } catch (IOException e) {
            System.out.println("Error in staging");
            return;
        }
    }


    //Read serialized Staging area.
    static Staging read(String directory) throws IllegalArgumentException {

        try {
            File stagingArea = Utils.join(directory, "staging");
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(stagingArea));
            Staging result = (Staging) in.readObject();
            in.close();
            return result;

        } catch (IOException e) {
            throw new IllegalArgumentException();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException();
        }

    }
    // Serialize stage.
    void close() {
        try {
            Staging obj = this;
            File outfile = stagingAreaFile;
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outfile));
            out.writeObject(obj);
            out.close();
        } catch (IOException e) {
            System.out.println("Error in saving stage");
            return;
        }

    }
    // Create a new commit instance and save it to the repo. Save files in staging area to repo.
    protected void commit(String message) {

        if (currMap.isEmpty() && removeMap.isEmpty()) {
            System.out.println("No changes added to the commit.");

        } else {

            this.finalizeMap();
            Commit comm = new Commit(headCommit, message, parentMap);
            headCommit = comm.getID();
            setHeadCommit(activeBranch);
            changeBranchMap(parentMap, activeBranch);
            addBranchCommit(comm.getID(), activeBranch);
            allCommits.add(comm.getID());
            comm.finalize();
        }
    }

    // Update parentMap using currMap.
    private void finalizeMap() {
        parentMap.putAll(currMap);
        for (String key : removeMap.keySet()) {
            parentMap.remove(key);
        }

        removeMap.clear();
        currMap.clear();
    }


    // Create new branch.
    protected void branch(String name) {
        if (allBranchNames.contains(name)) {
            System.out.println("A branch with that name already exists.");
        } else {
            allBranchNames.add(name);
            Branch newBranch = new Branch(name, headCommit, parentMap);
            newBranch.saveBranch();
            Branch active = Branch.load(activeBranch);
            active.splitCommit = headCommit;
            active.saveBranch();
        }

    }

    //Add file to staging area.
    void add(File file, String name) {

        if (!Utils.plainFilenamesIn(Repo.directory).contains(name)) {
            System.out.println("File does not exist.");
            return;
        }

        byte[] blob = Utils.readContents(file);
        String hash = Utils.sha1(blob);

        if (removeMap.containsKey(name)) {
            removeMap.remove(name);
            return;

        } else if (parentMap.containsKey(name) && parentMap.get(name).equals(hash)) {
            return;

        } else if (currMap.containsKey(name)) {
            if (currMap.get(name).equals(hash)) {
                return;
            } else {

                File temp = getGitletDir(currMap.get(name));
                temp.delete();

                currMap.replace(name, hash);
                Utils.writeContents(getGitletDir(hash), blob);
            }
        } else {

            Utils.writeContents(getGitletDir(hash), blob);
            currMap.put(name, hash);
        }

    }

    //Remove file.
    void remove(String filename) {

        if (currMap.containsKey(filename)) {
            File target = getGitletDir(currMap.get(filename));
            target.delete();
            currMap.remove(filename);

        } else if (parentMap.containsKey(filename)) {
            removeMap.put(filename, parentMap.get(filename));
            Utils.restrictedDelete(Repo.directory + filename);

        } else {
            System.out.println("No reason to remove the file.");
        }

    }
    //Checkout to most recent commit.
    protected void checkout(String filename) {
        Branch active = Branch.load(activeBranch);

        if (!active.branchContents.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
        } else {
            String name = active.branchContents.get(filename);
            File source = new File(getGitletPath() + name);
            byte[] contents = Utils.readContents(source);
            Utils.writeContents(new File(Repo.directory + filename), contents);
        }

        active.saveBranch();

    }

    //Checkout to specified commit ID.
    protected void checkout(String id, String filename) {
        try {
            Branch active = Branch.load(activeBranch);

            for (String ids : active.branchCommits) {
                if (ids.length() <= 40 && ids.substring(0, id.length()).equals(id)) {
                    id = ids;
                }
            }


            Commit sourceCommit = Commit.load(id);
            HashMap<String, String> sourceMap = sourceCommit.getContents();

            if (!active.branchCommits.contains(id) || !sourceMap.containsKey(filename)) {
                System.out.println("File does not exist in that commit.");
            } else {
                String name = sourceMap.get(filename);
                File source = new File(getGitletPath() + name);
                byte[] contents = Utils.readContents(source);
                Utils.writeContents(new File(Repo.directory + filename), contents);
            }

            active.saveBranch();
            sourceCommit.finalize();

        } catch (IllegalArgumentException e) {
            System.out.println("No commit with that id exists.");
            return;
        }
    }
    //Checkout to specified branch.
    protected void checkoutBranch(String branchName) {

        if (!allBranchNames.contains(branchName)) {
            System.out.println("No such branch exists.");
        } else if (activeBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
        } else {
            Branch targetBranch = Branch.load(branchName);
            Branch active = Branch.load(activeBranch);
            List<String> workingDir = Utils.plainFilenamesIn(Repo.directory);

            for (String file : workingDir) {
                if (!parentMap.containsKey(file) && targetBranch.branchContents.containsKey(file)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                    return;
                }
            }

            for (String file : active.branchContents.keySet()) {
                if (!targetBranch.branchContents.containsKey(file)) {
                    Utils.restrictedDelete(Repo.directory + file);
                    parentMap.remove(file);
                }

            }
            parentMap.putAll(targetBranch.branchContents);
            currMap.clear();
            removeMap.clear();

            for (String key : targetBranch.branchContents.keySet()) {
                String name = targetBranch.branchContents.get(key);
                File source = new File(getGitletPath() + name);
                byte[] contents = Utils.readContents(source);
                Utils.writeContents(new File(Repo.directory + key), contents);

            }

            headCommit = targetBranch.headCommitId;
            targetBranch.branchContents = parentMap;
            activeBranch = branchName;
            targetBranch.saveBranch();
            active.saveBranch();
        }

    }

    //Remove specified branch.
    public void removeBranch(String branchName) {
        if (!allBranchNames.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (branchName.equals(activeBranch)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            Branch b = Branch.load(branchName);
            b.headCommitId = null;
            b.saveBranch();
        }
    }
    //Print status.
    public void status() {

        System.out.println("=== Branches ===");
        System.out.println("*" + activeBranch);
        for (String branch : allBranchNames) {
            if (!branch.equals(activeBranch)) {
                System.out.println(branch);
            }
        }
        System.out.println();


        System.out.println("=== Staged Files ===");
        for (String adding : currMap.keySet()) {
            System.out.println(adding);
        }
        System.out.println();


        System.out.println("=== Removed Files ===");
        for (String removing : removeMap.keySet()) {
            System.out.println(removing);
        }
        System.out.println();


        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
    }

    //Print log of active branch.
    protected void log() {
        Branch active = Branch.load(activeBranch);
        String tempCommitTrack = active.headCommitId;
        active.saveBranch();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");


        while (tempCommitTrack != null) {

            Commit head = Commit.load(tempCommitTrack);

            System.out.println("===");
            System.out.println("Commit " + tempCommitTrack);
            System.out.println(format.format(head.getCommitDate()));
            System.out.println(head.getMessage());
            if (head.getParent() != null) {
                System.out.println();
            }

            tempCommitTrack = head.getParent();
            head.finalize();
        }
    }
    //Prints log of all commits made.
    protected void globalLog() {

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");


        for (String commitID : allCommits) {
            Commit currComm = Commit.load(commitID);

            System.out.println("===");
            System.out.println("Commit " + commitID);
            System.out.println(format.format(currComm.getCommitDate()));
            System.out.println(currComm.getMessage());
            if (currComm.getParent() != null) {
                System.out.println();
            }

            currComm.finalize();

        }
    }

    //Prints commitIDs corresponding to the input commit message.
    protected void find(String msg) {
        int counter = 0;

        for (String commitID : allCommits) {
            Commit currComm = Commit.load(commitID);
            if (currComm.getMessage().equals(msg)) {
                System.out.println(commitID);
                counter++;

            }
            currComm.finalize();

        }

        if (counter == 0) {
            System.out.println("Found no commit with that message.");
            return;
        }

    }

    //Reset to specified commit.
    protected void reset(String id) {
        Branch active = Branch.load(activeBranch);

        for (String ids : allCommits) {
            if (ids.length() <= 40 && ids.substring(0, id.length()).equals(id)) {
                id = ids;
            }
        }

        if (!allCommits.contains(id)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        HashMap<String, String> sourceMap = Commit.load(id).getContents();
        List<String> workingDir = Utils.plainFilenamesIn(Repo.directory);

        for (String file : workingDir) {
            if (!parentMap.containsKey(file) && sourceMap.containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return;
            }
        }

        for (String file : sourceMap.keySet()) {

            if (!parentMap.containsKey(file)) {
                Utils.restrictedDelete(Repo.directory + file);
                parentMap.remove(file);
            }
        }
        parentMap.putAll(sourceMap);
        active.branchContents = sourceMap;
        currMap.clear();
        removeMap.clear();


        for (String key : sourceMap.keySet()) {

            String name = sourceMap.get(key);
            File source = new File(getGitletPath() + name);
            byte[] contents = Utils.readContents(source);
            Utils.writeContents(new File(Repo.directory + key), contents);
        }

        headCommit = id;
        active.headCommitId = id;
        active.saveBranch();

    }

    //Merge error cases and base cases.
    protected void merge(String branchName) {
        if (!allBranchNames.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (!currMap.isEmpty() || !removeMap.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        } else if (branchName.equals(activeBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        List<String> workingDir = Utils.plainFilenamesIn(Repo.directory);
        Branch target = Branch.load(branchName);

        for (String file : workingDir) {
            if (!parentMap.containsKey(file) && target.branchContents.containsKey(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return;
            }
        }
        if (target.headCommitId.equals(target.splitCommit)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (headCommit.equals(target.splitCommit)) {
            Branch active = Branch.load(activeBranch);
            active.overwriteBranch(branchName);
            active.saveBranch();
            headCommit = target.headCommitId;
            System.out.println("Current branch fast-forwarded.");
            return;
        } else {
            specialMerge(branchName);
        }

    }

    //Merge conflicts and special cases.
    void specialMerge(String branchName) {
        Branch target = Branch.load(branchName);
        HashMap<String, String> splitMap = Commit.load(target.splitCommit).getContents();
        boolean conflict = false;

        for (String key : target.branchContents.keySet()) {
            if (!parentMap.containsKey(key) && !splitMap.containsKey(key)) {
                currMap.put(key, target.branchContents.get(key));
            } else if (parentMap.containsKey(key) && !splitMap.containsKey(key)) {
                File currFile = getGitletDir(parentMap.get(key));
                File targetFile = getGitletDir(target.branchContents.get(key));
                fileMerger(currFile, targetFile, Repo.directory + key);
                conflict = true;

            }
        }
        for (String key : splitMap.keySet()) {
            if (target.branchContents.containsKey(key)
                    && !splitMap.get(key).equals(target.branchContents.get(key))
                    && splitMap.get(key).equals(parentMap.get(key))) {
                if (parentMap.containsKey(key)
                        && splitMap.get(key).equals(parentMap.get(key))) {
                    currMap.put(key, target.branchContents.get(key));
                }
            } else if (parentMap.containsKey(key)
                    && splitMap.get(key).equals(parentMap.get(key))
                    && !target.branchContents.containsKey(key)) {
                remove(key);

            } else if (target.branchContents.containsKey(key)
                    && parentMap.containsKey(key)
                    && !splitMap.get(key).equals(parentMap.get(key))
                    && !splitMap.get(key).equals(target.branchContents.get(key))) {

                if (!target.branchContents.get(key).equals(parentMap.get(key))) {
                    File targetFile = getGitletDir(target.branchContents.get(key));
                    File currFile = getGitletDir(parentMap.get(key));
                    fileMerger(currFile, targetFile, Repo.directory + key);
                    conflict = true;
                }
                // Modified in curr, absent from given
            } else if (!target.branchContents.containsKey(key)
                    && parentMap.containsKey(key)
                    && !splitMap.get(key).equals(parentMap.get(key))) {

                File currFile = getGitletDir(parentMap.get(key));
                oneFileMerger(currFile, Repo.directory + key, true);
                conflict = true;

                // Modified in target, absent from curr.
            } else if (!parentMap.containsKey(key)
                    && target.branchContents.containsKey(key)
                    && !splitMap.get(key).equals(target.branchContents.get(key))) {

                File targetFile = getGitletDir(target.branchContents.get(key));
                oneFileMerger(targetFile, Repo.directory + key, false);
                conflict = true;
            }
        }

        for (String key : currMap.keySet()) {
            String name = currMap.get(key);
            File source = new File(getGitletPath() + name);
            byte[] contents = Utils.readContents(source);
            Utils.writeContents(new File(Repo.directory + key), contents);
        }

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
            return;
        } else {
            commit("Merged " + activeBranch + " with " + branchName + ".");
        }
    }

    //Merges two files.
    void fileMerger(File file1, File file2, String filename) {
        byte[] file1Contents = Utils.readContents(file1);
        byte[] file2Contents = Utils.readContents(file2);
        String file1String = new String(file1Contents);
        String file2String = new String(file2Contents);

        String start = "<<<<<<< HEAD\n";
        String mid = "=======\n";
        String end = ">>>>>>>\n";

        String result = start + file1String + mid + file2String + end;

        Utils.writeContents(new File(filename), result.getBytes());
    }

    //Merges file to empty file.
    void oneFileMerger(File file1, String filename, boolean curr) {
        byte[] file1Contents = Utils.readContents(file1);
        String file1String = new String(file1Contents);
        String result;

        String start = "<<<<<<< HEAD\n";
        String mid = "=======\n";
        String end = ">>>>>>>\n";
        if (curr) {
            result = start + file1String + mid + end;
        } else {
            result = start + mid + file1String + end;
        }

        Utils.writeContents(new File(filename), result.getBytes());
    }





    //Get file in .gitlet directory
    File getGitletDir(String filename) {
        String[] path = {".gitlet/" + filename};
        return Utils.join(Repo.directory, path);
    }


    //Get current Map.
    protected HashMap<String, String> getMap() {
        return this.currMap;
    }

    //Get .gitlet path
    String getGitletPath() {
        return Repo.directory + ".gitlet/";
    }

    //Reset head commit in active branch.
    void setHeadCommit(String activeB) {
        Branch active = Branch.load(activeB);
        active.headCommitId = headCommit;
        active.saveBranch();
    }

    // Setter for active branch.
    void setActiveBranch(String branchName) {
        activeBranch = branchName;
    }

    // Changes branches' contents.
    void changeBranchMap(HashMap<String, String> map, String active) {
        Branch activeB = Branch.load(active);
        activeB.branchContents = map;
        activeB.saveBranch();
    }

    // Adds commit to branches' commits.
    void addBranchCommit(String id, String active) {
        Branch activeB = Branch.load(active);
        activeB.branchCommits.add(id);
        activeB.saveBranch();
    }

}
