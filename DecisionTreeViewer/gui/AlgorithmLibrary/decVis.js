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

var fileName = "../chesscategories.txt";
var text = FileHelper.readStringFromFileAtPath(fileName);
var categoryNames = text.split(",");

var url = "../chesstree.xml";
var xml = new JKL.ParseXML(url);
var data = xml.parse();
var root = data.tree.root;
var atts = data.tree.attributes.attribute;
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
    this.insertButton = addControlToAlgorithmBar("Button", "New Attributes");
    this.insertButton.onclick = this.generateAttributesCaller.bind(this);
    this.categories = [];

}

decVis.prototype.reset = function()
{
    this.nextIndex = 1;
    this.treeRoot = null;
}

decVis.prototype.showDecision = function() {
    this.commands = new Array();
    var node = this.treeRoot;
    var parentNode;
    while (node.isLeaf == "false") {
        parentNode = node;
        attLabel = this.categories[parseInt(node.attID)];
        this.cmd("SetHighlight", node.graphicID, 1);
        this.cmd("SetHighlight", attLabel.graphicID, 1);

        this.cmd("Step");
        if (node.split.isCategorical == "true" && node.split.equalTo == attLabel.val ||
                node.split.isCategorical == "false" && parseFloat(node.split.lessOrEqualTo) <= parseFloat(attLabel.val))
        {

            this.cmd("SetEdgeHighlight", node.graphicID, node.trueChild.graphicID, 1);
            node = node.trueChild;
        } else
        {
            this.cmd("SetEdgeHighlight", node.graphicID, node.falseChild.graphicID, 1);
            node = node.falseChild;
        }

        this.cmd("Step");
        this.cmd("SetHighlight", parentNode.graphicID, 0);
        this.cmd("SetEdgeHighLight", parentNode.graphicID, node.graphicID, 0);
        this.cmd("SetHighlight", attLabel.graphicID, 0);
        this.cmd("Step");
    }
    this.cmd("SetHighlight", node.graphicID, 1);
    this.cmd("Step");
    this.cmd("Step");
    this.cmd("Step");
    this.cmd("Step");
    this.cmd("Step");
    this.cmd("SetHighlight", node.graphicID, 0);
    return this.commands;
}
decVis.prototype.insertCallback = function(event)
{
    this.implementAction(this.showDecision.bind(this));
}

decVis.prototype.generateAttributes = function() {
    for (var i = 0; i < this.categories.length; i++) {
        this.cmd("Delete", this.categories[i].graphicID);
    }
    this.categories = [];
    for (var i = 0; i < categoryNames.length; i++) {
        var val;
        if (atts[i].isCategorical == "true") {
            rnd = Math.floor(Math.random() * atts[i].category.length);
            val = atts[i].category[rnd].value;
        } else {
            min = parseFloat(atts[i].minValue);
            max = parseFloat(atts[i].maxValue);
            rnd = Math.random() * (max - min) + min;
            val = rnd.toFixed(2);
        }
        var catNode = categoryNode(this.nextIndex++, categoryNames[i], val);
        this.categories.push(catNode);
        this.cmd("CreateLabel", this.categories[i].graphicID, i + ": " + this.categories[i].att + "  =  " + val, 0, 30 + i * 12, 0);
    }
    return this.commands;
}
decVis.prototype.buildTree = function()
{
    this.commands = new Array();
    this.generateAttributes();
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
        var data = "";
        if (tree.isLeaf == "false") {
            tree.attID = tree.split.attributeId;
            data = tree.attID;
            if (tree.split.isCategorical == "false") {
                data += "\n<=\n" + parseFloat(tree.split.lessOrEqualTo).toFixed(2);
            } else if (tree.split.isCategorical == "true") {
                data += "\n=\n" + tree.split.equalTo;
            }
        } else {
            var maxCount = -1;
            for (var i = 0; i < tree.classCounts.classCount.length; i++)
            {
                var classCategory = tree.classCounts.classCount[i].classCategory;
                var classCount = tree.classCounts.classCount[i].count;
                if (classCount > maxCount) {
                    leafName = classCategory;
                    maxCount = classCount;
                }
            }
            data = leafName + "\n";
            tree.attID = null;
        }

        tree.graphicID = this.nextIndex++;
        data = data + "\n";
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
function categoryNode(id, att, val)
{
    var O = Object();
    O.att = att;
    O.graphicID = id;
    O.val = val;
    return O;
}

decVis.prototype.drawTree = function(val) {
    this.implementAction(this.buildTree.bind(this));
}
decVis.prototype.generateAttributesCaller = function() {
    this.commands = new Array();
    var oldIndex = this.nextIndex;
    this.nextIndex = 0;
    this.implementAction(this.generateAttributes.bind(this));
    this.nextIndex = oldIndex;
}

var currentAlg;

function init()
{
    var animManag = initCanvas();
    currentAlg = new decVis(animManag, canvas.width, canvas.height);
    currentAlg.drawTree("3");

}