// Copyright 2011 David Galles, University of San Francisco. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without modification, are
// permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
// conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice, this list
// of conditions and the following disclaimer in the documentation and/or other materials
// provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY David Galles ``AS IS'' AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
// FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUdecVisITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// The views and conclusions contained in the software and documentation are those of the
// authors and should not be interpreted as representing official policies, either expressed
// or implied, of the University of San Francisco

function FileHelper()
{
}
{
    FileHelper.readStringFromFileAtPath = function(pathOfFileToReadFrom)
    {
        var request = new XMLHttpRequest();
        request.open("GET", pathOfFileToReadFrom, false);
        request.send(null);
        var returnValue = request.responseText;
        return returnValue;
    }
}

var fileName = "../wdcbcategories.txt";
var text = FileHelper.readStringFromFileAtPath(fileName);
var categoryNames = text.split(",");

var url = "../chesstree.xml";
var xml = new JKL.ParseXML(url);
var data = xml.parse();
var root = data.tree.root;
/*
 function expand(node) {
 //		for (var i=0;i<node.classCounts.classCount.length;i++)
 //		{
 //			var classCategory = node.classCounts.classCount[i].classCategory;
 //			var classCount = node.classCounts.classCount[i].count;
 //
 //		}
 if(node.isLeaf == "false") {
 var SplitAttribute = categoryNames[node.split.attributeId];
 var TrueCondition;
 var FalseCondition;
 if(node.split.isCategorical == "false"){
 TrueCondition =  "<=" + parseFloat(node.split.lessOrEqualTo).toFixed(2);
 } else{
 TrueCondition = node.split.equalTo;
 }
 if(node.split.isCategorical == "false"){
 FalseCondition =  ">" + parseFloat(node.split.lessOrEqualTo).toFixed(2);
 } else{
 FalseCondition = "not " + node.split.equalTo;
 }
 
 expand(node.trueChild);
 expand(node.falseChild);
 } else {
 var leafName;
 var maxCount = -1;
 for (var i=0;i<node.classCounts.classCount.length;i++)
 {
 var classCategory = node.classCounts.classCount[i].classCategory;
 var classCount = node.classCounts.classCount[i].count;
 if(classCount > maxCount){
 leafName = classCategory;
 maxCount = classCount;
 }
 }
 }
 }
 
 expand(root);
 */
// Constants.





function decVis(am, w, h)
{
    this.init(am, w, h);
}


decVis.prototype = new Algorithm();
decVis.prototype.constructor = decVis;
decVis.superclass = Algorithm.prototype;

decVis.LINK_COLOR = "#007700";
decVis.HIGHLIGHT_CIRCLE_COLOR = "#007700";
decVis.FOREGROUND_COLOR = "#007700";
decVis.BACKGROUND_COLOR = "#EEFFEE";
decVis.PRINT_COLOR = decVis.FOREGROUND_COLOR;

decVis.WIDTH_DELTA = 100;
decVis.HEIGHT_DELTA = 100;
decVis.STARTING_Y = 50;


decVis.FIRST_PRINT_POS_X = 50;
decVis.PRINT_VERTICAL_GAP = 40;
decVis.PRINT_HORIZONTAL_GAP = 50;

decVis.prototype.init = function(am, w, h)
{
    var sc = decVis.superclass;
    this.startingX = w / 2;
    this.first_print_pos_y = h - 2 * decVis.PRINT_VERTICAL_GAP;
    this.print_max = w - 10;
    this.treeRoot = root;
    this.treeRoot.x = this.startingX;
    this.treeRoot.y = decVis.STARTING_Y;

    var fn = sc.init;
    fn.call(this, am);
    this.addControls();
    this.nextIndex = 0;
    this.commands = [];
    this.animationManager.StartNewAnimation(this.commands);
    this.animationManager.skipForward();
    this.animationManager.clearHistory();
}

decVis.prototype.addControls = function()
{
    this.insertButton = addControlToAlgorithmBar("Button", "Decide");
    this.insertButton.onclick = this.insertCallback.bind(this);
}

decVis.prototype.reset = function()
{
    this.nextIndex = 1;
    this.treeRoot = null;
}
decVis.prototype.deleteTree = function() {

}

decVis.prototype.showDecision = function() {
    this.commands = new Array();
    var node = this.treeRoot;
    var parentNode;
    while (node.isLeaf == "false") {
        parentNode = node;
        this.cmd("SetHighlight", node.graphicID, 1);
        this.cmd("Step");
        if (false) //check whether attribute passes/fails split
        {
            this.cmd("SetEdgeHighlight", node.graphicID,node.trueChild.graphicID, 1);
            node = node.trueChild;
        } else {
            this.cmd("SetEdgeHighlight", node.graphicID,node.falseChild.graphicID, 1);
            node = node.falseChild;
        }
        this.cmd("Step");
        this.cmd("SetHighlight", parentNode.graphicID, 0);
        this.cmd("SetEdgeHighLight",parentNode.graphicID,node.graphicID,0);
        this.cmd("Step");
    }
    return this.commands;
}
decVis.prototype.insertCallback = function(event)
{
    this.implementAction(this.showDecision.bind(this));
}


decVis.prototype.buildTree = function()
{
    this.commands = new Array();

    var startingPoint = this.startingX;
    this.resizeWidths(this.treeRoot);
    if (this.treeRoot != null)
    {
        if (this.treeRoot.leftWidth > startingPoint)
        {
            startingPoint = this.treeRoot.leftWidth;
        }
        else if (this.treeRoot.rightWidth > startingPoint)
        {
            startingPoint = Math.max(this.treeRoot.leftWidth, 2 * startingPoint - this.treeRoot.rightWidth);
        }
        this.setNewPositions(this.treeRoot, startingPoint, decVis.STARTING_Y, 0);
        this.animateNewPositions(this.treeRoot);
        this.cmd("Step");
    }
    this.connectTree(this.treeRoot);
    return this.commands;
}
decVis.prototype.connectTree = function(elem)
{
    if (elem.isLeaf == "false") {
        this.cmd("Connect", elem.graphicID, elem.trueChild.graphicID, "#000000", 0.0, true, "T");
        this.cmd("Connect", elem.graphicID, elem.falseChild.graphicID, "#000000", 0.0, true, "F");
        this.connectTree(elem.trueChild);
        this.connectTree(elem.falseChild);
    }
}


decVis.prototype.setNewPositions = function(tree, xPosition, yPosition, side)
{
    if (tree != null)
    {
        tree.y = yPosition;
        if (side == -1)
        {
            xPosition = xPosition - tree.rightWidth;
        }
        else if (side == 1)
        {
            xPosition = xPosition + tree.leftWidth;
        }
        tree.x = xPosition;
        this.setNewPositions(tree.falseChild, xPosition, yPosition + decVis.HEIGHT_DELTA, -1)
        this.setNewPositions(tree.trueChild, xPosition, yPosition + decVis.HEIGHT_DELTA, 1)
    }

}
decVis.prototype.animateNewPositions = function(tree)
{
    if (tree != null)
    {
        var data = tree.id + ":\n";
        if (tree.isLeaf == "false" && tree.split.isCategorical == "false") {
            data += "<= " + parseFloat(tree.split.lessOrEqualTo).toFixed(2);
        } else if(tree.isLeaf == "false" && tree.split.isCategorical == "true") {
            data += "= " + tree.split.equalTo;
        }
        tree.graphicID = this.nextIndex++;
        this.cmd("CreateRectangle", tree.graphicID, data, 50, 50, tree.x, tree.y);
        this.cmd("SetPosition", tree.graphicID, tree.x, tree.y);
        this.animateNewPositions(tree.falseChild);
        this.animateNewPositions(tree.trueChild);
    }
}

decVis.prototype.resizeWidths = function(tree)
{
    if (tree == null)
    {
        return 0;
    }
    tree.leftWidth = Math.max(this.resizeWidths(tree.falseChild), decVis.WIDTH_DELTA / 2);
    tree.rightWidth = Math.max(this.resizeWidths(tree.trueChild), decVis.WIDTH_DELTA / 2);
    return tree.leftWidth + tree.rightWidth;
}


function decVisNode(val, id, initialX, initialY)
{
    this.data = val;
    this.x = initialX;
    this.y = initialY;
    this.graphicID = id;
    this.left = null;
    this.right = null;
    this.parent = null;
}

decVis.prototype.drawTree = function(val) {
    this.implementAction(this.buildTree.bind(this));
}

var currentAlg;

function init()
{
    var animManag = initCanvas();
    currentAlg = new decVis(animManag, canvas.width, canvas.height);
    currentAlg.drawTree("3");

}