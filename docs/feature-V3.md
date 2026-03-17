## AI Analyser
The goal of the feature is to select slack chanel(s) for analysing and daily job will try summerise based on interests and provide summery about each channel. if in the same channel different topics were discussed each of them must be summerized separatly. 


### UI components
In UI we should have a section to see the JOB running history , when it run how many channels are succeded and failed is there running process (non completed) . 
Refferecne link to the slack channel(s)

Separate section to see Summeries from analyse per day and per channel
Links to reference slack channel (if specific message refference available we should refer to that conversation from which summery is generated.)


### Configuration part
Job running should be defined (daily or manual)
Ex: every day at 10 AM it runs , or manually user hit run
In case if summery already done (in DB we should have that info that last time scanned channel ) manual or job run process will finished earlier as there is nothing to scan. 

We should be able to configure how scanning should be processed. 
This is about to having categoriesed, labeled summeries.
Summeries may contain a lot of links. 
For now , we may do it manually but in the future app should decide and create new task and assign to appropriate project automatically. 

Example: 
- in some thread someone requested code review on specific project
Automatically created Task in that project
- someone asking info / documentation related for example how to use api to achive some result . 
Automatically creates Task in that project
- Monitoring alert which is not resolved or resolved late 
Automatically creates Task in that project


Cases can be a lot

### System TTL
We should show only last 7th day's summeries only. after that it will be arhchived. 
We will keep summeries onluy for last 30 days.

### Architeture

This is feature that must be applied as a plugin. In the future we will have different plugins and we will download and apply to this project. so let's think about having plugin attaching mechanism . and build this project/task like a add-on to any project.  
