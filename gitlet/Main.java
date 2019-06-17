package gitlet;


import java.io.File;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */

public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */

    static String gitletDir = System.getProperty("user.dir") + "/.gitlet/";
    static String workingDir = System.getProperty("user.dir") + "/";


    static void init(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands");
            return;
        }

        File f = new File(gitletDir);
        if (f.exists()) {
            System.out.println("A gitlet version-control system "
                    + "already exists in the current directory.");
            return;

        } else {
            Repo repository = new Repo();
            repository.init();
            repository.setStage();
        }
    }

    static void add(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands");
            return;
        }
        try {
            String filename = args[1];
            Staging curr = Staging.read(gitletDir);

            File file = new File(workingDir + filename);
            curr.add(file, filename);
            curr.close();

        } catch (IllegalArgumentException e) {
            System.out.println("IO Exception at add");
            return;
        }
    }

    static void commit(String[] args) {
        if (args.length > 2) {
            System.out.println("Incorrect operands");
            return;
        } else if (args.length == 1 || args[1].equals("")) {
            System.out.println("Please enter a commit message.");
        }
        try {
            String msg = args[1];
            Staging curr = Staging.read(gitletDir);
            curr.commit(msg);
            curr.close();
        } catch (IllegalArgumentException e) {
            System.out.println("IO Exception");
        }
    }

    static void rm(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands");
            return;
        }
        try {
            String filename = args[1];
            Staging curr = Staging.read(gitletDir);
            curr.remove(filename);
            curr.close();
        } catch (IllegalArgumentException e) {
            System.out.println("IO Exception at rm");
        }
    }

    static void log(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands");
            return;
        } else {
            Staging curr = Staging.read(gitletDir);
            curr.log();
            curr.close();
        }
    }

    static void gLog(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            return;
        } else {
            Staging curr = Staging.read(gitletDir);
            curr.globalLog();
            curr.close();
        }
    }

    static void find(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String commitmsg = args[1];
            Staging curr = Staging.read(gitletDir);
            curr.find(commitmsg);
            curr.close();
        }
    }

    static void status(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
        } else {
            Staging curr = Staging.read(gitletDir);
            curr.status();
            curr.close();
        }
    }

    static void checkout(String[] args) {
        if (args.length > 4) {
            System.out.println("Incorrect operands.");
            return;
        }

        if (args[1].equals("--")) {
            String filename = args[2];
            Staging curr = Staging.read(gitletDir);
            curr.checkout(filename);
            curr.close();

        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            String id = args[1];
            String filename = args[3];
            Staging curr = Staging.read(gitletDir);
            curr.checkout(id, filename);
            curr.close();

        } else {
            String branchName = args[1];
            Staging curr = Staging.read(gitletDir);
            curr.checkoutBranch(branchName);
            curr.close();

        }
    }

    static void branch(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands");
            return;
        }

        try {
            String branchname = args[1];
            Staging curr = Staging.read(gitletDir);
            curr.branch(branchname);
            curr.close();

        } catch (IllegalArgumentException e) {
            System.out.println("Must initialize!");
            return;
        }
    }

    static void rmBranch(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            String remBranch = args[1];
            Staging curr = Staging.read(gitletDir);
            curr.removeBranch(remBranch);
            curr.close();
        }
    }

    static void reset(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            Staging curr = Staging.read(gitletDir);
            curr.reset(args[1]);
            curr.close();
        }

    }

    static void merge(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            Staging curr = Staging.read(gitletDir);
            curr.merge(args[1]);
            curr.close();
        }
    }



    public static void main(String... args) {

        if (args.length == 0) {
            System.out.println("Please enter a command");
            return;
        } else if (!args[0].equals("init")) {
            File f = new File(gitletDir);
            if (!f.exists()) {
                System.out.println("Not in an initialized gitlet directory.");
            }
        }

        String command = args[0];
        switch (command) {
            case "init":
                init(args);
                break;

            case "add":
                add(args);
                break;

            case "commit":
                commit(args);
                break;

            case "rm":
                rm(args);
                break;

            case "log":
                log(args);
                break;

            case "global-log":
                gLog(args);
                break;

            case "find":
                find(args);
                break;

            case "status":
                status(args);
                break;

            case "checkout":
                checkout(args);
                break;

            case "branch":
                branch(args);
                break;

            case "rm-branch":
                rmBranch(args);
                break;

            case "reset":
                reset(args);
                break;

            case "merge":
                merge(args);
                break;

            default:
                System.out.println("No command with that name exists.");
                return;
        }
    }
}
