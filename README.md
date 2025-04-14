# Quick And Dirty Static Page Generator

Least fancy way of generating HTML files from MD source

# Build

````lein uberjar````

# Usage
from command line:

````
java -jar path/to/uberjar source-dir dest-dir base-html

````

from within clojure:

```` 
(comment 
    (-main "-s"  "/code/content/" "-d" "/code/public/"  "--base" "/code/template/base.html")
nil
)
````
where,
source-dir : contains your source .md files
dest-dir : where you want your HTML files outputed, for example "./public/"
base-html Path to the HTML templete which would be used to create static pages.

# Base HTML

````
<body>
    <h1> {{title}} </h1>

    <div>
    {{content}}
    </div>
</body>

````
# sample md

```` 
---
title: This is the Title
key: value
---
# Heading1

main content goes here
````


# frontmatter
Any key-value pair part of the frontmatter will be read and inserted into the base HTML
