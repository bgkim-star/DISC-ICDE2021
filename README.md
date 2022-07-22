# DISC Algorithm 

## Introduction
- [`DISC`](https://doi.org/10.1109/ICDE51399.2021.00077) is an incremental density-based clustering algoritm for data streams (sliding window model).   
- It supports `collect` and `cluster` operations to incrementally update clusters over data streams. 
  - The `collect` operation collects data points to be updated. 
  - The `cluster` operation updates clusters from the collected data points. 
- It supports `labelAndReturn` to compute the labels of the data points in the window. 
- It returns the same clustering result as DBSCAN


## How to Use
### Prerequisite 
- maven 

### Build  
- `mvn package -DskipTests`
- JAR file will be created in the `target` folder. 

### Test
- Copy or soft link `sample_dataset` folder in `DenForest-SIGMOD2021 git` to the DISC-ICDE2021 directory
- Run `mvn test` // Run several tests using `sample_dataset`.

### Example Code 
You can find example codes in following files. 
- ` src/test/java/example/DISC_test.java`
- ` src/test/java/disc/DISC_Denforest_optimized_test.java`

### Sample Test Code 
You can find test codes in following files. 
- ` src/test/java/disc/DISCTest.java`

## Directory Overview 
```
DISC-ICDE2021
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── disc
    │   │       ├── DBSCAN_options.java
    │   │       ├── DISC.java                       // DISC algorithm with MS-BFS code
    │   │       ├── epochbasedrtree                 // Epoch_based_rtree for efficient data point retrievals.
    │   │       │   ├── Element.java
    │   │       │   ├── Epoch_Based_Rtree.java      
    │   │       │   ├── MBR.java
    │   │       │   └── Node.java       
    │   │       ├── Point.java        
    │   │       └── unionfind                      // unionfind
    │   │           └── UnionFind.java
    │   └── resources
    └── test
        └── java
            ├── disc
            │   └── DISCTest.java             // test code using the below example
            └── example
                └── DISC_test.java            // example code for DISC

```

## Reference
B. Kim, K. Koo, J. Kim and B. Moon,  "DISC: Density-Based Incremental Clustering by Striding over Streaming Data," in 2021 IEEE 37th International Conference on Data Engineering (ICDE), Chania, Greece, 2021 pp. 828-839.
doi: 10.1109/ICDE51399.2021.00077
keywords: {data analysis;computational modeling;scalability;conferences;clustering algorithms;tools;data engineering}
url: https://doi.ieeecomputersociety.org/10.1109/ICDE51399.2021.00077
###
