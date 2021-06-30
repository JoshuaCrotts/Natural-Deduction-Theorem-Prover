package com.ndtp.algorithms;

import com.ndtp.models.treenode.WffTree;

import java.util.ArrayList;

public interface WffAlgorithm {

    ArrayList<WffTree> getList(WffTree tree);

    WffTree get(WffTree tree);
}
