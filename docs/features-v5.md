## Functional Engancements

### Task Detail page

- we have a lot of features , currenly all of them are available in any task. we should make them configurable. 
In case if local LLM profile is not setup there is no need to show Generate Description or Generate Summery buttons. 
The same is valid for any feature which is not configured properly. It means that we should have application session scope to store all feature availability before showing the usage part. Once the config is updated session scopped data will be updated and features will be available

### Activity Logs
- Ability to archive/delete activities. cometimes it just a noise. we should be able to cleanup any time if needed.


## UI Changes
UI design is not good for me , we need to have fresh design. 
different icons for different cases, 
project, task ,workspace , branch .... almost all concepts and components may have icons. 
we need instead of big blue buttons have buttons. if overall UI concept needs to be changes let's do that but would like to have better UI


### Start
- Let's add banner/loader for app. as application should do a lot of checks to create a session let's at first add banner to show that Ai Task is loaded. 
currently don't have a banner image for it but use something we will update it later. also add progress bar. at least 10 sec should be loaded. 
- currently app opening smaller. it should get the desktop size and load full screen mode.



### Left sidebar
Let's add icons 

### Task Detail page 
- Let's use Icons . for example Run with Codex button is too big. no optimal use of the space. we may have Codex Icon which will done the same thing. 
There are a lot of cases where icons can be used. 
- When all features are available for the task the detail page is too big and it is hard to find the wanted section. I am suggesting to provide colapse/expand functionality or tabs to switch between features. 
example:  (we may use icons here as well)
methodoly part will have bmad part
Ai Assitents ; Gepppa, Ai Agents
updates - Activity logs related to tasks
settings - we don't have seeting for now but I'm sure we will have some  

The main functionaltiy will be without colapse expand (status change , open with etc..) in case of tabs it will be main tab
The same concept can be used for Project Detail page




