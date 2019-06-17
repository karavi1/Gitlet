
package gitlet;
import java.io.Serializable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Date;




class Commit implements Serializable {

    Commit(String parentId, String message, HashMap<String, String> map) {
        this.parent = parentId;
        this.message = message;
        this.contents = map;
        this.commitDate = new Date();

        ID = this.makeCommitID();
    }

    Commit() {
        this.message = "initial commit";
        this.parent = null;
        this.commitDate = new Date();
        ID = this.makeCommitID();
    }

    /** The SHA-1 identifier of my parent, or null if I am the initial commit. */
    private final String parent;
    /** The SHA-1 identifier of the current object **/
    private String ID;
    /** My log message. */
    private final String message;
    /** My timestamp. (java.util.Date) */
    private Date commitDate;
    /** A mapping of file names to the SHA-1's of their blobs. */
    private HashMap<String, String> contents = new HashMap<>();
    /** A static variable which holds the pointer to the HEAD commit **/
    protected static String head;


    // Methods
    public String toString() {
        return ID + "\n" + commitDate + "\n" + message;
    }

    /** Get SHA-1 identifier of my parent, or null if I am the initial commit. */
    String getParent() {
        return parent;
    }

    //Make ID of commit.
    public String makeCommitID() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(this);
            objectStream.close();

            return Utils.sha1(stream.toByteArray());
        } catch (IOException excp) {
            return ("Internal error serializing commit.");
        }
    }

    //Get ID of commit
    public String getID() {
        return ID;
    }


    // Finalizes, writes to repo.
    public void finalize() {
        try {
            Commit obj = this;
            File outfile = getFilePath(ID);
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outfile));
            out.writeObject(obj);
            out.close();
        } catch (IOException e) {
            System.out.println("Error in serialization");
            return;
        }
    }

    //Returns filepath for specified id.
    public File getFilePath(String id) {
        String[] path = {".gitlet/", "commits/", id};
        return Utils.join(Repo.directory, path);
    }

    //Loads commit object.
    protected static Commit load(String id) throws IllegalArgumentException {

        try {
            File tempStaging = Utils.join(Repo.directory, ".gitlet/", "commits/", id);
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(tempStaging));
            Commit result = (Commit) in.readObject();
            in.close();
            return result;

        } catch (IOException e) {
            throw new IllegalArgumentException();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException();
        }

    }
    //Returns commit contents.
    protected HashMap<String, String> getContents() {
        return contents;
    }

    //Returns commit timestamp.
    protected Date getCommitDate() {
        return commitDate;
    }

    //Returns commit message.
    protected String getMessage() {
        return message;
    }
}



