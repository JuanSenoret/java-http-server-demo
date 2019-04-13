import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CommentsWallComponent } from './comments-wall.component';

describe('CommentsWallComponent', () => {
  let component: CommentsWallComponent;
  let fixture: ComponentFixture<CommentsWallComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CommentsWallComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CommentsWallComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
