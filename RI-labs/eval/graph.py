import matplotlib.pyplot as plt
import numpy as np
from matplotlib.font_manager import FontProperties
from matplotlib.ticker import FormatStrFormatter
import sys


def readRecallValues(fileName):
    print(fileName)
    with open(fileName) as f:
        lines = f.readlines()
        del lines[:10]
    return parseRecall(lines[:11])


def parseRecall(recallLines):
    recall_y=[]
    for r in recallLines:
        recall_y.append(r.split()[2])
    return recall_y


def createGraphFromFile(fileName):
    recallValues= readRecallValues(fileName)
    plotGraph(recallValues, fileName)

def plotGraph(recallValuesMap):
    x_values = []
    for x in range(0,11):
        x_values.append(x*10)

    print(x_values)
    figg = plt.figure()
    axx = figg.add_subplot(211)
    for file in recallValuesMap:
        print(recallValuesMap[file])
        axx.plot(x_values, recallValuesMap[file], 'kx')
        axx.plot(x_values, recallValuesMap[file], '',  label=file)

    handles, labels = axx.get_legend_handles_labels()
    lgdd = axx.legend(handles, labels, loc='upper center', bbox_to_anchor=(0.5,-0.1))
    axx.grid('on')
    figg.show()
    figg.savefig('recall_graph', bbox_extra_artists=(lgdd,), bbox_inches='tight')

def createFileValuesMap(fileName):
    fileValuesMap={}
    for file in fileNames:
        fileValuesMap[file.split(".")[0]]= readRecallValues(file)
    return fileValuesMap 

plotGraph(createFileValuesMap(sys.argv[1:]))
