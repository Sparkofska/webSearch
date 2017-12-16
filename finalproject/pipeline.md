
# Pipeline

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
