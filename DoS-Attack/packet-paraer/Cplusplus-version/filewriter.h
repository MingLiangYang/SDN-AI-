#include <iostream>
#include <stdio.h>

using namespace std;

class Filewriter
{
public:
void writeToFile(char content[], char path[])
{
	FILE *fp = NULL;
 	fp = fopen(path, "a+");
	fprintf(fp, "%s", content);
	fclose(fp);
}
};
