var InterblueScope=null;
//var InterBluApp = angular.module('InterBlu', ['ngAnimate']);
var InterBluApp = angular.module('InterBlu', []);






InterBluApp.controller('MainController', ['$scope', '$timeout', '$http',
function($scope, $timeout, $http) {
	$scope.UIobj={};


	$scope.UIobj.LoopCount=0;
	InterblueScope=$scope;
	$scope.BTRECVData ="";
	
	
	$scope.LoadCMDSets = function(ObjName,filePath) {
		
		$http.get(filePath).
			success(function(data, status, headers, config) {
				$scope[ObjName]=data;
			}).
			error(function(data, status, headers, config) {
			  console.log( status);
			  // log error
			});
	};
	$scope.LoadCMDSets("CMD","datasets/commands/CMD.json");

	$scope.AndroidRECVData = function(msg){
		$scope.$apply(function () {
            $scope.RECVData(msg);
        });
	};



	$scope.RECVData = function(msg) {

		console.log(msg);
		var dataObj = JSON.parse(msg);

		dataObj.rawData = window.atob(dataObj.base64Data);
		dataObj.base64Data=null;


		if(dataObj.chid.toString().startsWith('ScanEv')&&!dataObj.rawData.startsWith('null'))
		{
			var obj={};
			obj.name=dataObj.rawData;
			$scope.UIobj.scanDevList.push(obj);

		}


		$scope.AppendRECVData(dataObj);
    };

	var BTRECVDataArr=[];
	$scope.AppendRECVData = function(dataObj) {


		var str=dataObj.chid.toString(16)+">>"+dataObj.rawData;
		
		BTRECVDataArr.push(str);
		if(BTRECVDataArr.length>100)
			BTRECVDataArr.shift();

		//$scope.BTRECVData.push(msg);
		$scope.BTRECVData=BTRECVDataArr.join("\n");

	};

	//SendBinMsg2BT($scope.gpCMD.CMDIDMaster,$scope.gpCMD.GetSetting.GetSetting);







	$scope.SendDBinMsg2BT = function(CH,uint8Arr) {
		var dataObj={};
		dataObj.chid=CH;
		dataObj.uint8Arr=uint8Arr;
		JsIF_BLEMaster.SendMsg2BT(JSON.stringify(dataObj));
	};
	$scope.SendDMsg2BT = function(CH,msg) {
		var base64Data=window.btoa(msg);
		var dataObj={};
		dataObj.chid=CH;
		dataObj.base64Data=base64Data;
		JsIF_BLEMaster.SendMsg2BT(JSON.stringify(dataObj));
	};

	$scope.SendMsg2BT = function(CH,msg) {
		var chid=parseInt(CH, 16);
		var base64Data=window.btoa(msg);
		var dataObj={};
		dataObj.chid=chid;
		dataObj.base64Data=base64Data;
		
		JsIF_BLEMaster.SendMsg2BT(JSON.stringify(dataObj));
	};

	$scope.SendBinMsg2BT = function(CH,uint8Arr) {

		var chid=parseInt(CH, 16);
		var dataObj={};
		dataObj.chid=chid;
		dataObj.uint8Arr=uint8Arr;
		JsIF_BLEMaster.SendMsg2BT(JSON.stringify(dataObj));

	};
}]).directive('ngEnter', function() {
	return function(scope, element, attrs) {
		element.bind("keydown keypress", function(event) {
			if (event.which === 13) {
				scope.$apply(function() {
					scope.$eval(attrs.ngEnter);
				});

				event.preventDefault();
			}
		});
	};
});

