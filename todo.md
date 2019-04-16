# letsdo.events

## Tech

- login/signup click without js
- signup with password
- login with password
- signup email only
- login email only
- figwheel don't watch everything
- production build
- include tachyons locally
- error page
- security headers
- gzip
- html meta tags
- production router
- cache css
- js as progressive enhancement

## Login

- authenticate via mail
- signup with password

## New Topic

- create a topic
- suggest activity/talk/thing
- title, description, (requirements)
- max number of attendees
- possible date field
- upload picture

## List Events

- filter activities to organizing and signed up for
- search/filter by name/description
- join

## later

- event edit view
- topic settings page
- use your own colors and logo
- add introduction text
- undo join
- cancel invite
- setup possible dates
- Filter by organizer
- list all your topics
- anyone can join/invite only
- delete topic
- list users
- make someone admin
- change username
- delete account
- single topic mode
- schedule/commit
- edit only by organizer
- multiple organizers per event
- list public topics
- request joining topic
- login theme depending on goto parameter
- logout
-
- Gravatar
- notify about new activity (via mail)

## later later

- calendar integration
- API keys
- json export

## Data Model

- topic: name, activity/talk/event
- user: name, email
- invite: email
- admin: topic -> user
- topic-location: topic -> location
- location: title
- activity: title, description, requirements, work-in-progress, start-time, end-time, location, organizer
- topic-activity: topic -> activity
- activity-organizer: user -> activity
- topic-participant: user -> topic
- activity-participant: user -> activity
- login-token: user, token, created-at
- delete event

## Routes

## Pages

### Signup

letsdo.events/login?goto=:route -> login/signup -> POST form

    letsdo.events/login?goto=heart-of-clojure/about/cljdoc-hacking

### Create Topic

letsdo.events/new -> create event -> POST form

### Topic

letsdo.events/for/heart-of-clojure/new -> create event
letsdo.events/for/:topic -> view topic
letsdo.events/for/:topic/about/:event -> view+edit event
letsdo.events/for/:topic/whoareyou -> set user name

### API

POST /api/invite
POST /api/event
PUT /api/event
PUT /api/me
POST /api/join


## Tools

- clojure
- postgres
- tools.deps
- reitit
- tachyons
- hiccup
- garden
- buddy