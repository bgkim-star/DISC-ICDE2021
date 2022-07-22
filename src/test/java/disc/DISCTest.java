package disc;
import example.DISC_test;
import org.junit.Test;
public class DISCTest {


        @Test
        public void test1(){
            System.out.println("[Test1] Run DenForest Test");

            DISC_test test = new DISC_test();

            System.out.println("[Test1-1]");

            String[] args1 = {"./sample_dataset","1", "5", "0.002", "100000", "5000", "5"};
            test.run(args1);

            System.out.println("[Test1-2]");

            String[] args2 = {"./sample_dataset","2", "5", "0.002", "50000", "5000", "5"};
            test.run(args2);

            System.out.println("[Test1-3]");

            String[] args3 = {"./sample_dataset","3", "5", "2", "50000", "5000", "5"};
            test.run(args3);

            System.out.println("[Test1-4]");

            String[] args4 = {"./sample_dataset","4", "5", "0.3", "50000", "5000", "5"};
            test.run(args4);

            System.out.println("[Test1] Run DenForest Complete!");

        }

}
