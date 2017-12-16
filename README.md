# Final Project

See code in Folder finalproject

## Pipeline

1. parse tweets stream -> seperate to different daily result sets
2. build index (daily basis)
3. parse profiles
4. construct queries from profiles
5. submit query, retrieve top relevant documents
6. pseudo relevance feedback query (enhance initial query)
7. Clustering by K-Means and MinHash
8. pick top diverse results
9. rank fusion: 

# TODO

[ ] add Analyzers (for index and queries)
[ ] build query from Profiles
[ ] (Dusan) check how evaluation works (.py script)
[ ] submitting query and retrieve docs in proper way
[ ] (Dasa)how to implement K-Means
[ ] (Jonas)and MinHash on Documents?
    - [MinHash for dummies](http://matthewcasperson.blogspot.pt/2013/11/minhash-for-dummies.html)
    - [Implementation](http://www.sanfoundry.com/java-program-implement-min-hash/)
    - [LHS-Implementation](https://github.com/tdebatty/java-LSH)

## Notes

 - read project guidelines
 - use Twitter4J for parsing JSON files (if we want)
 - split interest profiles in train and test
 - evaluation using multi-level-relevance
 - diverse digest: remove similar/redundant tweets and present the same amount of information in a more compressed way (nuggets)
 - repeat and improve the first two checkpoints (especially the Anlyzers: combine a few, not only test  3 different)
 - already reports + 2 more parts (Rank fusion and Diversity of information)
      - follow lab guide for rank fusion

# webSearch Lab

## To run
Add the files
 - eval/Answers.csv
 - eval/Questions.csv

## Sources
 - [Learn how to use trec_eval to evaluate your information retrieval system](http://www.rafaelglater.com/en/post/learn-how-to-use-trec_eval-to-evaluate-your-information-retrieval-system)
 - [Lucene Scoring](http://www.lucenetutorial.com/advanced-topics/scoring.html)
 - [Using built-in Analyzers](http://javabeat.net/using-the-built-in-analyzers-in-lucene/)



