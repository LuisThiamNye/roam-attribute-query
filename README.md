# Roam Research Query based on Attributes

Just playing around. Might be useful.

To use this, copy the code from [here](https://github.com/LuisThiamNye/roam-attribute-query/blob/master/src/attr_query/core.cljs) into a Clojure codeblock in Roam. To use the component:

- `{{[[roam/render]]: ((block-ref-to-codeblock))}}`
  - ?e
  - ?e [[attribute]] [[any]] [[of]] [[these]]
  - [[page]] [[attribute2]] ?e

Currently supports very limited Datalog-style querying. The first nested block defines the symbol representing a matching page/block (`?e`). After that, the blocks on the first level of indentation define the conditions that the result must satisfy.

It can also match based on a string:
- ?e [[year published]] 2010

Click the button to execute the query.

![](https://pbs.twimg.com/media/EuHwsCAWgAk5o9h?format=png&name=900x900)

Threads of thought:
https://twitter.com/LuisThiamNye/status/1360633555626393602

If anyone is interested in using something like this, please let me know so I can take this more seriously.
