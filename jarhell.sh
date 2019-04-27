
grep 'Dependency convergence' out|sed -e 's/Dependency convergence error for //g ; s/ paths.*//g'|sort -u| sed -e 's/^/<exclusion><groupId>/g; s|:[0-9].*|</artifactId></exclusion>|g; s|:|</groupId><artifactId>|g'

