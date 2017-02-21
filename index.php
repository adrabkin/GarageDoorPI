<?php 
	if(isset($_GET['trigger']) && $_GET['trigger'] == 1) {
		error_reporting(E_ALL);
		exec('gpio mode 8 out');
		usleep(1000000);
		exec('gpio mode 8 in');
	}
	
	if(isset($_GET['trigger']) && $_GET['trigger'] == 2) {
		error_reporting(E_ALL);
		exec('gpio mode 9 out');
		usleep(1000000);
		exec('gpio mode 9 in');
		
	}
	
	if(isset($_GET['trigger']) && $_GET['trigger'] == 3) {
		error_reporting(E_ALL);
		exec('gpio mode 7 out');
		usleep(1000000);
		exec('gpio mode 7 in');
		
	}
	
?>
